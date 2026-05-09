package com.josephyusuf.admin.service;

import com.josephyusuf.admin.dto.*;
import com.josephyusuf.admin.entity.PromoCode;
import com.josephyusuf.admin.exception.PromoCodeAlreadyExistsException;
import com.josephyusuf.admin.exception.PromoCodeException;
import com.josephyusuf.admin.exception.PromoCodeNotFoundException;
import com.josephyusuf.admin.mapper.PromoCodeMapper;
import com.josephyusuf.admin.repository.PromoCodeRepository;
import com.josephyusuf.admin.repository.PromoCodeUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private PromoCodeUsageRepository promoCodeUsageRepository;

    @Mock
    private PromoCodeMapper promoCodeMapper;

    @InjectMocks
    private PromoCodeService service;

    private UUID adminId;
    private UUID userId;
    private PromoCode promo;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        userId = UUID.randomUUID();
        promo = PromoCode.builder()
                .id(UUID.randomUUID())
                .code("JOSEPH20")
                .discountPercent(20)
                .maxUses(100)
                .usedCount(0)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("create - rejects duplicate code")
    void create_duplicate() {
        PromoCodeRequest request = PromoCodeRequest.builder()
                .code("joseph20")
                .discountPercent(20)
                .build();

        when(promoCodeRepository.existsByCode("JOSEPH20")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request, adminId))
                .isInstanceOf(PromoCodeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("create - normalizes code and saves")
    void create_nominal() {
        PromoCodeRequest request = PromoCodeRequest.builder()
                .code("joseph20")
                .discountPercent(20)
                .maxUses(100)
                .build();

        when(promoCodeRepository.existsByCode("JOSEPH20")).thenReturn(false);
        when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promoCodeMapper.toResponse(any(PromoCode.class))).thenReturn(PromoCodeResponse.builder()
                .code("JOSEPH20").discountPercent(20).build());

        PromoCodeResponse response = service.create(request, adminId);

        assertThat(response.getCode()).isEqualTo("JOSEPH20");
        verify(promoCodeRepository).save(any(PromoCode.class));
    }

    @Test
    @DisplayName("validate - returns invalid for unknown code")
    void validate_unknown() {
        when(promoCodeRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        PromoCodeValidation result = service.validate("unknown", userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("inconnu");
    }

    @Test
    @DisplayName("validate - returns invalid for expired code")
    void validate_expired() {
        promo.setExpiresAt(Instant.now().minusSeconds(60));
        when(promoCodeRepository.findByCode("JOSEPH20")).thenReturn(Optional.of(promo));

        PromoCodeValidation result = service.validate("JOSEPH20", userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("expiré");
    }

    @Test
    @DisplayName("validate - returns invalid for exhausted code")
    void validate_exhausted() {
        promo.setMaxUses(5);
        promo.setUsedCount(5);
        when(promoCodeRepository.findByCode("JOSEPH20")).thenReturn(Optional.of(promo));

        PromoCodeValidation result = service.validate("JOSEPH20", userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("épuisé");
    }

    @Test
    @DisplayName("validate - returns invalid if user already used")
    void validate_alreadyUsed() {
        when(promoCodeRepository.findByCode("JOSEPH20")).thenReturn(Optional.of(promo));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(promo.getId(), userId)).thenReturn(true);

        PromoCodeValidation result = service.validate("JOSEPH20", userId);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReason()).contains("déjà utilisé");
    }

    @Test
    @DisplayName("validate - returns valid for active unused code")
    void validate_valid() {
        when(promoCodeRepository.findByCode("JOSEPH20")).thenReturn(Optional.of(promo));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(promo.getId(), userId)).thenReturn(false);

        PromoCodeValidation result = service.validate("JOSEPH20", userId);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getDiscountPercent()).isEqualTo(20);
    }

    @Test
    @DisplayName("apply - increments usage count and creates usage record")
    void apply_nominal() {
        when(promoCodeRepository.findByCode("JOSEPH20")).thenReturn(Optional.of(promo));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndUserId(promo.getId(), userId)).thenReturn(false);

        service.apply(PromoCodeApplyRequest.builder()
                .code("JOSEPH20").userId(userId).transactionId("tx_123").build());

        assertThat(promo.getUsedCount()).isEqualTo(1);
        verify(promoCodeUsageRepository).save(any());
        verify(promoCodeRepository).save(promo);
    }

    @Test
    @DisplayName("apply - throws if validation fails")
    void apply_invalid() {
        promo.setActive(false);
        when(promoCodeRepository.findByCode("JOSEPH20")).thenReturn(Optional.of(promo));

        assertThatThrownBy(() -> service.apply(PromoCodeApplyRequest.builder()
                .code("JOSEPH20").userId(userId).build()))
                .isInstanceOf(PromoCodeException.class);
    }

    @Test
    @DisplayName("toggle - flips active state")
    void toggle_nominal() {
        when(promoCodeRepository.findById(promo.getId())).thenReturn(Optional.of(promo));
        when(promoCodeRepository.save(promo)).thenReturn(promo);
        when(promoCodeMapper.toResponse(promo)).thenReturn(PromoCodeResponse.builder().build());

        service.toggle(promo.getId());

        assertThat(promo.isActive()).isFalse();
    }

    @Test
    @DisplayName("toggle - throws if not found")
    void toggle_notFound() {
        UUID id = UUID.randomUUID();
        when(promoCodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle(id))
                .isInstanceOf(PromoCodeNotFoundException.class);
    }
}
