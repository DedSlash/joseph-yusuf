package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayTechWebhookServiceTest {

    private static final String API_KEY = "test_api_key";
    private static final String API_SECRET = "test_api_secret";

    @Mock private TransactionRepository transactionRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private AuthClient authClient;
    @Mock private AdminClient adminClient;

    private PayTechConfig config;
    private PayTechWebhookService webhookService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        config = new PayTechConfig();
        config.setApiKey(API_KEY);
        config.setApiSecret(API_SECRET);
        objectMapper = new ObjectMapper();
        webhookService = new PayTechWebhookService(config, transactionRepository,
                subscriptionService, authClient, adminClient, objectMapper);
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private Map<String, Object> validPayload(String refCommand, UUID userId, String planTier,
                                              String couponCode) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type_event", "sale_complete");
        payload.put("ref_command", refCommand);
        payload.put("api_key_sha256", sha256(API_KEY));
        payload.put("api_secret_sha256", sha256(API_SECRET));

        Map<String, String> custom = new HashMap<>();
        custom.put("userId", userId.toString());
        custom.put("planTier", planTier);
        if (couponCode != null) custom.put("couponCode", couponCode);
        payload.put("custom_field", objectMapper.writeValueAsString(custom));
        return payload;
    }

    @Test
    @DisplayName("handleIPN sale_complete → activeAfterPayment + sync auth")
    void handleIPN_saleComplete_activates() throws Exception {
        UUID userId = UUID.randomUUID();
        String ref = "JY-12345678-99999";
        Map<String, Object> payload = validPayload(ref, userId, "PREMIUM", null);
        when(transactionRepository.findByTransactionId(ref)).thenReturn(Optional.empty());
        when(subscriptionService.activateAfterPayment(userId, PlanTier.PREMIUM,
                PaymentProvider.PAYTECH, ref))
                .thenReturn(Subscription.builder().userId(userId).build());

        webhookService.handleIPN(payload);

        verify(subscriptionService).activateAfterPayment(userId, PlanTier.PREMIUM,
                PaymentProvider.PAYTECH, ref);
        verify(authClient).updatePlan(any());
        verify(adminClient, never()).apply(any());
    }

    @Test
    @DisplayName("handleIPN sale_complete avec coupon → adminClient.apply appelé")
    void handleIPN_saleCompleteWithCoupon_appliesPromo() throws Exception {
        UUID userId = UUID.randomUUID();
        String ref = "JY-aabbccdd-12345";
        Map<String, Object> payload = validPayload(ref, userId, "PREMIUM", "EARLY50");
        when(transactionRepository.findByTransactionId(ref)).thenReturn(Optional.empty());
        when(subscriptionService.activateAfterPayment(any(), any(), any(), anyString()))
                .thenReturn(Subscription.builder().userId(userId).build());

        webhookService.handleIPN(payload);

        verify(adminClient).apply(any());
    }

    @Test
    @DisplayName("handleIPN signature invalide → SecurityException")
    void handleIPN_invalidSignature_throws() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type_event", "sale_complete");
        payload.put("api_key_sha256", "deadbeef");
        payload.put("api_secret_sha256", "deadbeef");

        assertThatThrownBy(() -> webhookService.handleIPN(payload))
                .isInstanceOf(SecurityException.class);

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("handleIPN type_event != sale_complete → ignoré silencieusement")
    void handleIPN_otherEvent_ignored() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type_event", "sale_pending");
        payload.put("api_key_sha256", sha256(API_KEY));
        payload.put("api_secret_sha256", sha256(API_SECRET));

        webhookService.handleIPN(payload);

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("handleIPN idempotence : ref déjà SUCCEEDED → pas de double activation")
    void handleIPN_alreadyProcessed_noDoubleActivation() throws Exception {
        UUID userId = UUID.randomUUID();
        String ref = "JY-aaaaaaaa-77777";
        Map<String, Object> payload = validPayload(ref, userId, "PREMIUM", null);

        Transaction tx = Transaction.builder()
                .transactionId(ref)
                .status(TransactionStatus.SUCCEEDED)
                .build();
        when(transactionRepository.findByTransactionId(ref)).thenReturn(Optional.of(tx));

        webhookService.handleIPN(payload);

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(authClient, never()).updatePlan(any());
    }

    @Test
    @DisplayName("handleIPN custom_field absent → ignoré sans crash")
    void handleIPN_noCustomField_ignored() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type_event", "sale_complete");
        payload.put("ref_command", "JY-noCustom-1");
        payload.put("api_key_sha256", sha256(API_KEY));
        payload.put("api_secret_sha256", sha256(API_SECRET));
        when(transactionRepository.findByTransactionId(anyString())).thenReturn(Optional.empty());

        webhookService.handleIPN(payload);

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("handleIPN custom_field passé comme Map directement → accepté")
    void handleIPN_customFieldAsMap_accepted() throws Exception {
        UUID userId = UUID.randomUUID();
        String ref = "JY-mapfield-1";
        Map<String, Object> payload = new HashMap<>();
        payload.put("type_event", "sale_complete");
        payload.put("ref_command", ref);
        payload.put("api_key_sha256", sha256(API_KEY));
        payload.put("api_secret_sha256", sha256(API_SECRET));

        Map<String, String> custom = new HashMap<>();
        custom.put("userId", userId.toString());
        custom.put("planTier", "PREMIUM_PLUS");
        payload.put("custom_field", custom);

        when(transactionRepository.findByTransactionId(ref)).thenReturn(Optional.empty());
        when(subscriptionService.activateAfterPayment(any(), any(), any(), anyString()))
                .thenReturn(Subscription.builder().userId(userId).build());

        webhookService.handleIPN(payload);

        verify(subscriptionService).activateAfterPayment(userId, PlanTier.PREMIUM_PLUS,
                PaymentProvider.PAYTECH, ref);
    }

    @Test
    @DisplayName("handleIPN apply coupon en erreur → silencieux (warning, pas d'exception)")
    void handleIPN_couponApplyFails_silentlyContinues() throws Exception {
        UUID userId = UUID.randomUUID();
        String ref = "JY-coupErr-1";
        Map<String, Object> payload = validPayload(ref, userId, "PREMIUM", "EARLY50");
        when(transactionRepository.findByTransactionId(ref)).thenReturn(Optional.empty());
        when(subscriptionService.activateAfterPayment(any(), any(), any(), anyString()))
                .thenReturn(Subscription.builder().userId(userId).build());
        when(adminClient.apply(any())).thenThrow(new RuntimeException("admin-service down"));

        webhookService.handleIPN(payload);

        verify(authClient).updatePlan(any());
    }
}
