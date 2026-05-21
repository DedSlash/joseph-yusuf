package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.*;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.service.OrangeMoneyService;
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

    @Mock private WaveService waveService;
    @Mock private OrangeMoneyService orangeMoneyService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private Authentication auth;

    @InjectMocks
    private SubscriptionController controller;

    private final UUID userId = UUID.randomUUID();

    private void mockAuth() {
        when(auth.getPrincipal()).thenReturn(userId.toString());
        lenient().when(auth.getDetails()).thenReturn("user@josephyusuf.com");
    }

    @Test
    @DisplayName("createStripeSubscription → 200")
    void createStripeSubscription() {
        mockAuth();
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .planTier(PlanTier.PREMIUM)
                .currency("EUR")
                .paymentMethodId("pm_card")
                .couponCode("EARLY50")
                .build();

        CreateSubscriptionResponse resp = CreateSubscriptionResponse.builder()
                .subscriptionId("sub_123")
                .clientSecret("pi_xxx_secret_xxx")
                .status("incomplete")
                .build();
        when(subscriptionService.createStripeSubscription(eq(userId), eq("user@josephyusuf.com"), any()))
                .thenReturn(resp);

        ResponseEntity<CreateSubscriptionResponse> response = controller.createStripeSubscription(auth, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getSubscriptionId()).isEqualTo("sub_123");
        assertThat(response.getBody().getClientSecret()).isEqualTo("pi_xxx_secret_xxx");
    }

    @Test
    @DisplayName("confirmStripeSubscription → 200")
    void confirmStripeSubscription() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder()
                .plan(PlanTier.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionService.confirmStripeSubscription(userId, "sub_123")).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.confirmStripeSubscription(auth, "sub_123");

        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("cancelStripeSubscription par défaut = cancel at period end")
    void cancelStripeSubscriptionDefault() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder().cancelAtPeriodEnd(true).build();
        when(subscriptionService.cancelStripeSubscription(userId, false)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.cancelStripeSubscription(auth, null);

        assertThat(response.getBody().isCancelAtPeriodEnd()).isTrue();
        verify(subscriptionService).cancelStripeSubscription(userId, false);
    }

    @Test
    @DisplayName("cancelStripeSubscription immediately=true")
    void cancelStripeSubscriptionImmediately() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder().status(SubscriptionStatus.CANCELLED).build();
        when(subscriptionService.cancelStripeSubscription(userId, true)).thenReturn(sub);

        CancelSubscriptionRequest req = CancelSubscriptionRequest.builder().immediately(true).build();
        ResponseEntity<SubscriptionResponse> response = controller.cancelStripeSubscription(auth, req);

        verify(subscriptionService).cancelStripeSubscription(userId, true);
        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
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
    @DisplayName("setAutoRenew → 200")
    void setAutoRenew() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder().autoRenew(true).build();
        when(subscriptionService.setAutoRenew(userId, true)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.setAutoRenew(auth, true);

        assertThat(response.getBody().isAutoRenew()).isTrue();
    }
}
