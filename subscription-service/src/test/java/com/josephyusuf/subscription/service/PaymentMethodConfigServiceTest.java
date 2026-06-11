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

    private PaymentMethodConfig cfg(PaymentProvider p, boolean enabled, String displayName,
                                     int order, String methodCode) {
        return PaymentMethodConfig.builder()
                .provider(p).enabled(enabled)
                .displayName(displayName).displayOrder(order)
                .paytechMethodCode(methodCode)
                .updatedAt(Instant.now()).build();
    }

    @Test
    @DisplayName("getAll → trie par displayOrder et mappe tout (admin)")
    void getAll_sortsAndMaps() {
        when(repository.findAll()).thenReturn(List.of(
                cfg(PaymentProvider.PAYTECH, false, "PayTech", 98, null),
                cfg(PaymentProvider.WAVE, true, "Wave", 1, "wave"),
                cfg(PaymentProvider.CARTE, true, "Carte bancaire", 4, "card")
        ));

        List<PaymentMethodConfigDto> result = service.getAll();

        assertThat(result).extracting(PaymentMethodConfigDto::getProvider)
                .containsExactly(PaymentProvider.WAVE, PaymentProvider.CARTE, PaymentProvider.PAYTECH);
        assertThat(result.get(0).getDisplayName()).isEqualTo("Wave");
        assertThat(result.get(0).getPaytechMethodCode()).isEqualTo("wave");
    }

    @Test
    @DisplayName("getAvailableForClient → enabled=true ET displayOrder<=97 (agrégateurs exclus)")
    void getAvailableForClient_filtersAndSorts() {
        when(repository.findAll()).thenReturn(List.of(
                cfg(PaymentProvider.PAYTECH, true, "PayTech", 98, null),       // exclus : agrégateur
                cfg(PaymentProvider.WAVE, true, "Wave", 1, "wave"),
                cfg(PaymentProvider.FREE_MONEY, false, "Free Money", 3, "free_money"), // exclus : disabled
                cfg(PaymentProvider.ORANGE_MONEY, true, "Orange Money", 2, "orange_money")
        ));

        List<PaymentMethodConfigDto> result = service.getAvailableForClient();

        assertThat(result).extracting(PaymentMethodConfigDto::getProvider)
                .containsExactly(PaymentProvider.WAVE, PaymentProvider.ORANGE_MONEY);
    }

    @Test
    @DisplayName("isEnabled → true si provider existant et enabled")
    void isEnabled_true() {
        when(repository.findById(PaymentProvider.WAVE)).thenReturn(Optional.of(
                cfg(PaymentProvider.WAVE, true, "Wave", 1, "wave")));
        assertThat(service.isEnabled(PaymentProvider.WAVE)).isTrue();
    }

    @Test
    @DisplayName("isEnabled → false si provider désactivé")
    void isEnabled_disabled() {
        when(repository.findById(PaymentProvider.PAYTECH)).thenReturn(Optional.of(
                cfg(PaymentProvider.PAYTECH, false, "PayTech", 98, null)));
        assertThat(service.isEnabled(PaymentProvider.PAYTECH)).isFalse();
    }

    @Test
    @DisplayName("isEnabled → false si provider absent")
    void isEnabled_notFound() {
        when(repository.findById(PaymentProvider.ORANGE_MONEY)).thenReturn(Optional.empty());
        assertThat(service.isEnabled(PaymentProvider.ORANGE_MONEY)).isFalse();
    }

    @Test
    @DisplayName("toggle → flip enabled sur config existante")
    void toggle_existing() {
        PaymentMethodConfig config = cfg(PaymentProvider.WAVE, false, "Wave", 1, "wave");
        when(repository.findById(PaymentProvider.WAVE)).thenReturn(Optional.of(config));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodConfigDto result = service.toggle(PaymentProvider.WAVE);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.WAVE);
    }

    @Test
    @DisplayName("toggle → crée nouvelle config si absente")
    void toggle_new() {
        when(repository.findById(PaymentProvider.ORANGE_MONEY)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentMethodConfigDto result = service.toggle(PaymentProvider.ORANGE_MONEY);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.ORANGE_MONEY);
    }
}
