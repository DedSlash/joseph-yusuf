package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.PaymentMethodConfigDto;
import com.josephyusuf.subscription.entity.PaymentMethodConfig;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.repository.PaymentMethodConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMethodConfigService")
class PaymentMethodConfigServiceTest {

    @Mock
    private PaymentMethodConfigRepository repository;

    @InjectMocks
    private PaymentMethodConfigService service;

    @Test
    @DisplayName("getAll → returns all configs mapped to DTOs")
    void getAll() {
        PaymentMethodConfig stripe = PaymentMethodConfig.builder()
                .provider(PaymentProvider.STRIPE).enabled(true).updatedAt(Instant.now()).build();
        PaymentMethodConfig wave = PaymentMethodConfig.builder()
                .provider(PaymentProvider.WAVE).enabled(false).updatedAt(Instant.now()).build();
        when(repository.findAll()).thenReturn(List.of(stripe, wave));

        List<PaymentMethodConfigDto> result = service.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(result.get(0).isEnabled()).isTrue();
        assertThat(result.get(1).getProvider()).isEqualTo(PaymentProvider.WAVE);
        assertThat(result.get(1).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("isEnabled → true when provider exists and enabled")
    void isEnabled_true() {
        PaymentMethodConfig config = PaymentMethodConfig.builder()
                .provider(PaymentProvider.STRIPE).enabled(true).build();
        when(repository.findById(PaymentProvider.STRIPE)).thenReturn(Optional.of(config));

        assertThat(service.isEnabled(PaymentProvider.STRIPE)).isTrue();
    }

    @Test
    @DisplayName("isEnabled → false when provider exists but disabled")
    void isEnabled_disabled() {
        PaymentMethodConfig config = PaymentMethodConfig.builder()
                .provider(PaymentProvider.STRIPE).enabled(false).build();
        when(repository.findById(PaymentProvider.STRIPE)).thenReturn(Optional.of(config));

        assertThat(service.isEnabled(PaymentProvider.STRIPE)).isFalse();
    }

    @Test
    @DisplayName("isEnabled → false when provider not found")
    void isEnabled_notFound() {
        when(repository.findById(PaymentProvider.ORANGE_MONEY)).thenReturn(Optional.empty());

        assertThat(service.isEnabled(PaymentProvider.ORANGE_MONEY)).isFalse();
    }

    @Test
    @DisplayName("toggle → flips enabled and returns DTO")
    void toggle_existing() {
        PaymentMethodConfig config = PaymentMethodConfig.builder()
                .provider(PaymentProvider.WAVE).enabled(false).updatedAt(Instant.now()).build();
        when(repository.findById(PaymentProvider.WAVE)).thenReturn(Optional.of(config));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodConfigDto result = service.toggle(PaymentProvider.WAVE);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.WAVE);
    }

    @Test
    @DisplayName("toggle → creates new config if not found")
    void toggle_new() {
        when(repository.findById(PaymentProvider.ORANGE_MONEY)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodConfigDto result = service.toggle(PaymentProvider.ORANGE_MONEY);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.ORANGE_MONEY);
    }
}
