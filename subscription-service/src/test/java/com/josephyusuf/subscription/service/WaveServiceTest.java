package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.OrangeMoneyRequest;
import com.josephyusuf.subscription.dto.PaymentProviderResponse;
import com.josephyusuf.subscription.dto.WavePaymentRequest;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaveServiceTest {

    private final WaveService waveService = new WaveService();
    private final OrangeMoneyService orangeService = new OrangeMoneyService();

    @Test
    @DisplayName("Wave : PREMIUM retourne 3000 XOF, PENDING")
    void wave_premium_returnsExpected() {
        WavePaymentRequest req = WavePaymentRequest.builder()
                .plan(PlanTier.PREMIUM).phoneNumber("+221770000000").build();

        PaymentProviderResponse response = waveService.initiate(UUID.randomUUID(), req);

        assertThat(response.getProvider()).isEqualTo(PaymentProvider.WAVE);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(response.getAmount()).isEqualByComparingTo("3000.00");
        assertThat(response.getCurrency()).isEqualTo("XOF");
        assertThat(response.getTransactionId()).startsWith("wave-sim-");
    }

    @Test
    @DisplayName("Wave : PREMIUM_PLUS retourne 6000 XOF")
    void wave_premiumPlus_returns6000() {
        WavePaymentRequest req = WavePaymentRequest.builder()
                .plan(PlanTier.PREMIUM_PLUS).phoneNumber("+221770000000").build();

        PaymentProviderResponse response = waveService.initiate(UUID.randomUUID(), req);

        assertThat(response.getAmount()).isEqualByComparingTo("6000.00");
    }

    @Test
    @DisplayName("Wave : FREE → InvalidPlanException")
    void wave_free_throws() {
        WavePaymentRequest req = WavePaymentRequest.builder()
                .plan(PlanTier.FREE).phoneNumber("+221770000000").build();

        assertThatThrownBy(() -> waveService.initiate(UUID.randomUUID(), req))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("OrangeMoney : PREMIUM retourne 3000 XOF, PENDING")
    void orange_premium_returnsExpected() {
        OrangeMoneyRequest req = OrangeMoneyRequest.builder()
                .plan(PlanTier.PREMIUM).phoneNumber("+221770000000").build();

        PaymentProviderResponse response = orangeService.initiate(UUID.randomUUID(), req);

        assertThat(response.getProvider()).isEqualTo(PaymentProvider.ORANGE_MONEY);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(response.getAmount()).isEqualByComparingTo("3000.00");
        assertThat(response.getTransactionId()).startsWith("om-sim-");
    }

    @Test
    @DisplayName("OrangeMoney : FREE → InvalidPlanException")
    void orange_free_throws() {
        OrangeMoneyRequest req = OrangeMoneyRequest.builder()
                .plan(PlanTier.FREE).phoneNumber("+221770000000").build();

        assertThatThrownBy(() -> orangeService.initiate(UUID.randomUUID(), req))
                .isInstanceOf(InvalidPlanException.class);
    }
}
