package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.*;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.service.OrangeMoneyService;
import com.josephyusuf.subscription.service.PayTechService;
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
    @Mock private PayTechService payTechService;
    @Mock private Authentication auth;

    @InjectMocks
    private SubscriptionController controller;

    private final UUID userId = UUID.randomUUID();

    private void mockAuth() {
        when(auth.getPrincipal()).thenReturn(userId.toString());
        lenient().when(auth.getDetails()).thenReturn("user@josephyusuf.com");
    }

    @Test
    @DisplayName("initiateWave → 200 + provider WAVE")
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
    @DisplayName("initiateOrange → 200 + provider ORANGE_MONEY")
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
    @DisplayName("createPayTechPayment → relaie paytechMethodCode au service")
    void createPayTechPayment_relaysMethodCode() {
        mockAuth();
        PayTechRequest request = PayTechRequest.builder()
                .planTier("PREMIUM").couponCode("EARLY50").paytechMethodCode("wave")
                .build();
        PayTechPaymentResponse resp = PayTechPaymentResponse.builder()
                .refCommand("JY-aaaa-1700")
                .redirectUrl("https://paytech.sn/checkout/x")
                .mobileRedirectUrl("https://paytech.sn/mobile/x")
                .build();
        when(payTechService.createPayment(userId, "PREMIUM", "EARLY50", "wave", null)).thenReturn(resp);

        ResponseEntity<PayTechPaymentResponse> response = controller.createPayTechPayment(auth, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getRefCommand()).isEqualTo("JY-aaaa-1700");
        verify(payTechService).createPayment(userId, "PREMIUM", "EARLY50", "wave", null);
    }

    @Test
    @DisplayName("createPayTechPayment sans method code → null relayé")
    void createPayTechPayment_nullMethodCode() {
        mockAuth();
        PayTechRequest request = PayTechRequest.builder().planTier("PREMIUM_PLUS").build();
        when(payTechService.createPayment(userId, "PREMIUM_PLUS", null, null, null))
                .thenReturn(PayTechPaymentResponse.builder().refCommand("JY-x").build());

        controller.createPayTechPayment(auth, request);

        verify(payTechService).createPayment(userId, "PREMIUM_PLUS", null, null, null);
    }

    @Test
    @DisplayName("current → 200 + plan retourné")
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
    @DisplayName("cancel par défaut immediately=false → cancelAtPeriodEnd")
    void cancelDefault() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder().cancelAtPeriodEnd(true).build();
        when(subscriptionService.cancelSubscription(userId, false)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.cancel(auth, false);

        assertThat(response.getBody().isCancelAtPeriodEnd()).isTrue();
        verify(subscriptionService).cancelSubscription(userId, false);
    }

    @Test
    @DisplayName("cancel immediately=true → status CANCELLED")
    void cancelImmediately() {
        mockAuth();
        SubscriptionResponse sub = SubscriptionResponse.builder().status(SubscriptionStatus.CANCELLED).build();
        when(subscriptionService.cancelSubscription(userId, true)).thenReturn(sub);

        ResponseEntity<SubscriptionResponse> response = controller.cancel(auth, true);

        verify(subscriptionService).cancelSubscription(userId, true);
        assertThat(response.getBody().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
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
