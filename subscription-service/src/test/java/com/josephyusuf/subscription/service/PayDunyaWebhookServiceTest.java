package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.dto.PayDunyaStatusResponse;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayDunyaWebhookServiceTest {

    @Mock private PayDunyaService payDunyaService;
    @Mock private SubscriptionService subscriptionService;
    @Mock private AuthClient authClient;
    @Mock private AdminClient adminClient;

    @InjectMocks private PayDunyaWebhookService webhookService;

    @Test
    @DisplayName("handleCallback completed → active l'abonnement et sync auth")
    void handleCallback_completed_activatesSubscription() {
        String token = "completed_token";
        UUID userId = UUID.randomUUID();

        Map<String, Object> customData = new HashMap<>();
        customData.put("userId", userId.toString());
        customData.put("planTier", "PREMIUM");
        customData.put("couponCode", null);

        when(payDunyaService.checkInvoiceStatus(token)).thenReturn(
                PayDunyaStatusResponse.builder()
                        .token(token)
                        .status("completed")
                        .customData(customData)
                        .build());

        when(subscriptionService.activateAfterPayment(userId, PlanTier.PREMIUM,
                PaymentProvider.PAYDUNYA, token))
                .thenReturn(Subscription.builder().userId(userId).build());

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);

        webhookService.handleCallback(payload);

        verify(subscriptionService).activateAfterPayment(userId, PlanTier.PREMIUM,
                PaymentProvider.PAYDUNYA, token);
        verify(authClient).updatePlan(any());
    }

    @Test
    @DisplayName("handleCallback completed avec coupon → apply le promo code")
    void handleCallback_completedWithCoupon_appliesPromo() {
        String token = "promo_token";
        UUID userId = UUID.randomUUID();

        Map<String, Object> customData = new HashMap<>();
        customData.put("userId", userId.toString());
        customData.put("planTier", "PREMIUM");
        customData.put("couponCode", "EARLY50");

        when(payDunyaService.checkInvoiceStatus(token)).thenReturn(
                PayDunyaStatusResponse.builder()
                        .token(token)
                        .status("completed")
                        .customData(customData)
                        .build());

        when(subscriptionService.activateAfterPayment(any(), any(), any(), anyString()))
                .thenReturn(Subscription.builder().userId(userId).build());

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);

        webhookService.handleCallback(payload);

        verify(adminClient).apply(any());
    }

    @Test
    @DisplayName("handleCallback failed → marque la transaction failed")
    void handleCallback_failed_marksTransactionFailed() {
        String token = "failed_token";

        when(payDunyaService.checkInvoiceStatus(token)).thenReturn(
                PayDunyaStatusResponse.builder()
                        .token(token)
                        .status("failed")
                        .customData(null)
                        .build());

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);

        webhookService.handleCallback(payload);

        verify(subscriptionService).markTransactionFailed(eq(token), anyString());
        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("handleCallback cancelled → marque la transaction failed")
    void handleCallback_cancelled_marksTransactionFailed() {
        String token = "cancelled_token";

        when(payDunyaService.checkInvoiceStatus(token)).thenReturn(
                PayDunyaStatusResponse.builder()
                        .token(token)
                        .status("cancelled")
                        .customData(null)
                        .build());

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);

        webhookService.handleCallback(payload);

        verify(subscriptionService).markTransactionFailed(eq(token), anyString());
    }

    @Test
    @DisplayName("handleCallback sans token → ignoré")
    void handleCallback_noToken_ignored() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("someField", "someValue");

        webhookService.handleCallback(payload);

        verify(payDunyaService, never()).checkInvoiceStatus(anyString());
    }

    @Test
    @DisplayName("handleCallback pending → rien à faire")
    void handleCallback_pending_noAction() {
        String token = "pending_token";

        when(payDunyaService.checkInvoiceStatus(token)).thenReturn(
                PayDunyaStatusResponse.builder()
                        .token(token)
                        .status("pending")
                        .customData(null)
                        .build());

        Map<String, Object> payload = new HashMap<>();
        payload.put("token", token);

        webhookService.handleCallback(payload);

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(subscriptionService, never()).markTransactionFailed(any(), any());
    }
}
