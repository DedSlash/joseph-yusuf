package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.*;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import org.mockito.ArgumentCaptor;
import com.josephyusuf.subscription.service.OrangeMoneyService;
import com.josephyusuf.subscription.service.StripeService;
import com.josephyusuf.subscription.service.SubscriptionService;
import com.josephyusuf.subscription.service.WaveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionController")
class SubscriptionControllerTest {

    @Mock private StripeService stripeService;
    @Mock private WaveService waveService;
    @Mock private OrangeMoneyService orangeMoneyService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private Authentication auth;

    @InjectMocks
    private SubscriptionController controller;

    private final UUID userId = UUID.randomUUID();

    private void mockAuth() {
        when(auth.getPrincipal()).thenReturn(userId.toString());
    }

    @Test
    @DisplayName("createStripePaymentIntent → 200")
    void createStripePaymentIntent() {
        mockAuth();
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setPlan(PlanTier.PREMIUM);
        request.setCurrency("EUR");

        PaymentIntentResponse resp = PaymentIntentResponse.builder()
                .paymentIntentId("pi_123")
                .clientSecret("sec_123")
                .amount(new BigDecimal("4.99"))
                .currency("EUR")
                .build();
        when(stripeService.createPaymentIntent(eq(userId), eq(PlanTier.PREMIUM), eq("EUR"), any())).thenReturn(resp);

        ResponseEntity<PaymentIntentResponse> response = controller.createStripePaymentIntent(auth, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPaymentIntentId()).isEqualTo("pi_123");
        ArgumentCaptor<PendingTransactionParams> captor = ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getPlan()).isEqualTo(PlanTier.PREMIUM);
        assertThat(captor.getValue().getProvider()).isEqualTo(PaymentProvider.STRIPE);
    }

    @Test
    @DisplayName("initiateWave → 200")
    void initiateWave() {
        mockAuth();
        WavePaymentRequest request = new WavePaymentRequest();
        request.setPlan(PlanTier.PREMIUM);
        request.setPhoneNumber("+221770000000");

        PaymentProviderResponse resp = PaymentProviderResponse.builder()
                .transactionId("wave_123")
                .amount(new BigDecimal("3000"))
                .currency("XOF")
                .provider(PaymentProvider.WAVE)
                .message("Confirmez sur votre téléphone")
                .build();
        when(waveService.initiate(eq(userId), any())).thenReturn(resp);

        ResponseEntity<PaymentProviderResponse> response = controller.initiateWave(auth, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getTransactionId()).isEqualTo("wave_123");
    }

    @Test
    @DisplayName("initiateOrange → 200")
    void initiateOrange() {
        mockAuth();
        OrangeMoneyRequest request = new OrangeMoneyRequest();
        request.setPlan(PlanTier.PREMIUM);
        request.setPhoneNumber("+221770000000");

        PaymentProviderResponse resp = PaymentProviderResponse.builder()
                .transactionId("om_123")
                .amount(new BigDecimal("3000"))
                .currency("XOF")
                .provider(PaymentProvider.ORANGE_MONEY)
                .message("Confirmez sur votre téléphone")
                .build();
        when(orangeMoneyService.initiate(eq(userId), any())).thenReturn(resp);

        ResponseEntity<PaymentProviderResponse> response = controller.initiateOrange(auth, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getTransactionId()).isEqualTo("om_123");
    }

    @Test
    @DisplayName("current → 200")
    void getCurrent() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder()
                .plan(PlanTier.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionService.getCurrent(userId)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.current(auth);

        assertThat(response.getBody().getPlan()).isEqualTo(PlanTier.PREMIUM);
    }

    @Test
    @DisplayName("confirmPayment → 200")
    void confirmPayment() {
        mockAuth();
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setPaymentIntentId("pi_123");

        SubscriptionResponse sub = SubscriptionResponse.builder()
                .plan(PlanTier.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionService.confirmStripePayment(userId, "pi_123")).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.confirmPayment(auth, request);

        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("setAutoRenew → 200")
    void setAutoRenew() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder().autoRenew(true).build();
        when(subscriptionService.setAutoRenew(userId, true)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.setAutoRenew(auth, true);

        assertThat(response.getBody().isAutoRenew()).isTrue();
    }

    @Test
    @DisplayName("cancel → 200")
    void cancel() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder()
                .status(SubscriptionStatus.CANCELLED)
                .build();
        when(subscriptionService.getCurrent(userId)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.cancel(auth);

        verify(subscriptionService).cancel(userId);
        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }
}
