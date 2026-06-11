package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.config.PaddleConfig;
import com.josephyusuf.subscription.entity.ProcessedWebhookEvent;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.repository.ProcessedWebhookEventRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaddleWebhookServiceTest {

    private static final String WEBHOOK_SECRET = "pdl_ntfset_test";

    @Mock private SubscriptionService subscriptionService;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ProcessedWebhookEventRepository processedWebhookEventRepository;
    @Mock private AuthClient authClient;

    private PaddleService paddleService;
    private PaddleWebhookService webhookService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PaddleConfig config = new PaddleConfig();
        config.setApiKey("k");
        config.setClientToken("c");
        config.setWebhookSecret(WEBHOOK_SECRET);
        config.setSandbox(true);
        config.setPrices(new PaddleConfig.Prices());
        paddleService = new PaddleService(config, null);
        objectMapper = new ObjectMapper();
        webhookService = new PaddleWebhookService(paddleService, subscriptionService,
                subscriptionRepository, processedWebhookEventRepository, authClient, objectMapper);
    }

    private String signedHeader(String body) throws Exception {
        long ts = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((ts + ":" + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return "ts=" + ts + ";h1=" + sb;
    }

    @Test
    @DisplayName("Signature invalide → SecurityException, aucun side-effect")
    void handle_badSignature_throws() {
        String body = "{\"event_id\":\"evt_1\",\"event_type\":\"transaction.completed\"}";
        assertThatThrownBy(() -> webhookService.handleWebhook(body, "ts=1;h1=deadbeef"))
                .isInstanceOf(SecurityException.class);
        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("transaction.completed → activate + persist paddle_* + sync auth + mark processed")
    void handle_transactionCompleted_activates() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = "{\"event_id\":\"evt_tc1\",\"event_type\":\"transaction.completed\","
                + "\"data\":{\"id\":\"txn_abc\",\"subscription_id\":\"sub_xyz\","
                + "\"customer_id\":\"ctm_111\","
                + "\"custom_data\":{\"userId\":\"" + userId + "\",\"planTier\":\"PREMIUM\"}}}";

        when(processedWebhookEventRepository.existsById("evt_tc1")).thenReturn(false);
        Subscription sub = Subscription.builder()
                .userId(userId).plan(PlanTier.PREMIUM).status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionService.activateAfterPayment(userId, PlanTier.PREMIUM,
                PaymentProvider.PADDLE, "txn_abc")).thenReturn(sub);

        webhookService.handleWebhook(body, signedHeader(body));

        ArgumentCaptor<Subscription> subCap = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subCap.capture());
        assertThat(subCap.getValue().getPaddleSubscriptionId()).isEqualTo("sub_xyz");
        assertThat(subCap.getValue().getPaddleCustomerId()).isEqualTo("ctm_111");

        verify(authClient).updatePlan(any());

        ArgumentCaptor<ProcessedWebhookEvent> evtCap =
                ArgumentCaptor.forClass(ProcessedWebhookEvent.class);
        verify(processedWebhookEventRepository).save(evtCap.capture());
        assertThat(evtCap.getValue().getEventId()).isEqualTo("evt_tc1");
        assertThat(evtCap.getValue().getProvider()).isEqualTo(PaymentProvider.PADDLE);
        assertThat(evtCap.getValue().getEventType()).isEqualTo("transaction.completed");
    }

    @Test
    @DisplayName("event déjà traité → no-op (idempotence)")
    void handle_alreadyProcessed_skips() throws Exception {
        String body = "{\"event_id\":\"evt_dup\",\"event_type\":\"transaction.completed\","
                + "\"data\":{\"id\":\"txn_x\",\"custom_data\":{\"userId\":\""
                + UUID.randomUUID() + "\",\"planTier\":\"PREMIUM\"}}}";
        when(processedWebhookEventRepository.existsById("evt_dup")).thenReturn(true);

        webhookService.handleWebhook(body, signedHeader(body));

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(processedWebhookEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("subscription.canceled → status CANCELLED + downgrade FREE + sync auth")
    void handle_subscriptionCanceled_downgrade() throws Exception {
        UUID userId = UUID.randomUUID();
        Subscription sub = Subscription.builder()
                .userId(userId).plan(PlanTier.PREMIUM).status(SubscriptionStatus.ACTIVE)
                .paddleSubscriptionId("sub_to_cancel").build();
        when(subscriptionRepository.findByPaddleSubscriptionId("sub_to_cancel"))
                .thenReturn(Optional.of(sub));
        String body = "{\"event_id\":\"evt_sc1\",\"event_type\":\"subscription.canceled\","
                + "\"data\":{\"id\":\"sub_to_cancel\"}}";
        when(processedWebhookEventRepository.existsById("evt_sc1")).thenReturn(false);

        webhookService.handleWebhook(body, signedHeader(body));

        ArgumentCaptor<Subscription> subCap = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subCap.capture());
        assertThat(subCap.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subCap.getValue().getPlan()).isEqualTo(PlanTier.FREE);
        assertThat(subCap.getValue().getCancelledAt()).isNotNull();
        verify(authClient).updatePlan(any());
    }

    @Test
    @DisplayName("subscription.updated → maj expiresAt et currentPeriodEnd depuis next_billed_at")
    void handle_subscriptionUpdated_extends() throws Exception {
        UUID userId = UUID.randomUUID();
        Subscription sub = Subscription.builder()
                .userId(userId).plan(PlanTier.PREMIUM).status(SubscriptionStatus.ACTIVE)
                .paddleSubscriptionId("sub_upd").build();
        when(subscriptionRepository.findByPaddleSubscriptionId("sub_upd"))
                .thenReturn(Optional.of(sub));
        String nextBilled = "2026-07-15T00:00:00Z";
        String body = "{\"event_id\":\"evt_up1\",\"event_type\":\"subscription.updated\","
                + "\"data\":{\"id\":\"sub_upd\",\"status\":\"active\","
                + "\"next_billed_at\":\"" + nextBilled + "\"}}";
        when(processedWebhookEventRepository.existsById("evt_up1")).thenReturn(false);

        webhookService.handleWebhook(body, signedHeader(body));

        ArgumentCaptor<Subscription> cap = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(cap.capture());
        assertThat(cap.getValue().getExpiresAt()).isEqualTo(Instant.parse(nextBilled));
        assertThat(cap.getValue().getCurrentPeriodEnd()).isEqualTo(Instant.parse(nextBilled));
        assertThat(cap.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("event_type non géré → marqué processed, pas d'action métier")
    void handle_unknownEventType_marksProcessedOnly() throws Exception {
        String body = "{\"event_id\":\"evt_unk\",\"event_type\":\"product.updated\",\"data\":{}}";
        when(processedWebhookEventRepository.existsById("evt_unk")).thenReturn(false);

        webhookService.handleWebhook(body, signedHeader(body));

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(subscriptionRepository, never()).save(any());
        verify(processedWebhookEventRepository).save(any());
    }

    @Test
    @DisplayName("transaction.completed sans custom_data → ignoré gracieusement")
    void handle_transactionCompleted_noCustomData_skipsActivation() throws Exception {
        String body = "{\"event_id\":\"evt_nc\",\"event_type\":\"transaction.completed\","
                + "\"data\":{\"id\":\"txn_y\"}}";
        when(processedWebhookEventRepository.existsById("evt_nc")).thenReturn(false);

        webhookService.handleWebhook(body, signedHeader(body));

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(processedWebhookEventRepository).save(any());
    }
}
