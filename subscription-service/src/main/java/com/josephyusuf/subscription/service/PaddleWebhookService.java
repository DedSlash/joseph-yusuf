package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.dto.PlanUpdateRequest;
import com.josephyusuf.subscription.entity.ProcessedWebhookEvent;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.repository.ProcessedWebhookEventRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaddleWebhookService {

    private final PaddleService paddleService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final AuthClient authClient;
    private final ObjectMapper objectMapper;

    /**
     * Point d'entrée principal pour le traitement des webhooks Paddle.
     */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (!paddleService.verifyWebhookSignature(payload, signatureHeader)) {
            throw new SecurityException("Paddle webhook signature invalide");
        }

        Map<String, Object> event = parsePayload(payload);
        String eventId = (String) event.get("event_id");
        String eventType = (String) event.get("event_type");

        if (eventId == null || eventType == null) {
            log.warn("Paddle webhook sans event_id ou event_type, ignoré");
            return;
        }

        // Idempotency check
        if (processedWebhookEventRepository.existsById(eventId)) {
            log.info("Paddle webhook déjà traité eventId={}, ignoré", eventId);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) event.get("data");
        if (data == null) {
            log.warn("Paddle webhook sans data eventId={}", eventId);
            return;
        }

        switch (eventType) {
            case "transaction.completed" -> handleTransactionCompleted(data);
            case "subscription.canceled" -> handleSubscriptionCancelled(data);
            case "subscription.updated" -> handleSubscriptionUpdated(data);
            default -> log.info("Paddle webhook eventType={} non géré, ignoré", eventType);
        }

        // Mark event as processed
        processedWebhookEventRepository.save(ProcessedWebhookEvent.builder()
                .eventId(eventId)
                .provider(PaymentProvider.PADDLE)
                .eventType(eventType)
                .build());

        log.info("Paddle webhook traité eventId={} eventType={}", eventId, eventType);
    }

    /**
     * transaction.completed : active la subscription et enregistre la transaction.
     */
    @SuppressWarnings("unchecked")
    private void handleTransactionCompleted(Map<String, Object> data) {
        String transactionId = (String) data.get("id");
        Map<String, Object> customData = (Map<String, Object>) data.get("custom_data");

        if (customData == null) {
            log.error("Paddle transaction.completed sans custom_data txId={}", transactionId);
            return;
        }

        String userIdStr = (String) customData.get("userId");
        String planTierStr = (String) customData.get("planTier");

        if (userIdStr == null || planTierStr == null) {
            log.error("Paddle transaction.completed custom_data incomplète txId={}", transactionId);
            return;
        }

        UUID userId = UUID.fromString(userIdStr);
        PlanTier plan = PlanTier.valueOf(planTierStr);

        // Activate subscription
        Subscription subscription = subscriptionService.activateAfterPayment(
                userId, plan, PaymentProvider.PADDLE, transactionId);

        // Update paddle fields if subscription_id present
        String paddleSubscriptionId = (String) data.get("subscription_id");
        String paddleCustomerId = extractCustomerId(data);
        if (paddleSubscriptionId != null) {
            subscription.setPaddleSubscriptionId(paddleSubscriptionId);
        }
        if (paddleCustomerId != null) {
            subscription.setPaddleCustomerId(paddleCustomerId);
        }
        subscriptionRepository.save(subscription);

        // Sync plan with auth-service
        syncPlanWithAuthService(userId, plan);

        log.info("Paddle transaction.completed activée userId={} plan={} txId={}",
                userId, plan, transactionId);
    }

    /**
     * subscription.canceled : marque la subscription comme CANCELLED.
     */
    private void handleSubscriptionCancelled(Map<String, Object> data) {
        String paddleSubscriptionId = (String) data.get("id");
        if (paddleSubscriptionId == null) {
            log.warn("Paddle subscription.canceled sans id");
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByPaddleSubscriptionId(paddleSubscriptionId).orElse(null);

        if (subscription == null) {
            log.warn("Paddle subscription.canceled — subscription locale introuvable paddleSubId={}",
                    paddleSubscriptionId);
            return;
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(Instant.now());
        subscription.setPlan(PlanTier.FREE);
        subscriptionRepository.save(subscription);

        syncPlanWithAuthService(subscription.getUserId(), PlanTier.FREE);

        log.info("Paddle subscription annulée userId={} paddleSubId={}",
                subscription.getUserId(), paddleSubscriptionId);
    }

    /**
     * subscription.updated : met à jour expiresAt depuis next_billed_at.
     */
    private void handleSubscriptionUpdated(Map<String, Object> data) {
        String paddleSubscriptionId = (String) data.get("id");
        if (paddleSubscriptionId == null) {
            log.warn("Paddle subscription.updated sans id");
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByPaddleSubscriptionId(paddleSubscriptionId).orElse(null);

        if (subscription == null) {
            log.warn("Paddle subscription.updated — subscription locale introuvable paddleSubId={}",
                    paddleSubscriptionId);
            return;
        }

        String nextBilledAt = (String) data.get("next_billed_at");
        if (nextBilledAt != null) {
            Instant nextBilled = Instant.parse(nextBilledAt);
            subscription.setExpiresAt(nextBilled);
            subscription.setCurrentPeriodEnd(nextBilled);
        }

        String status = (String) data.get("status");
        if ("active".equals(status)) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        } else if ("canceled".equals(status)) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(Instant.now());
        }

        subscriptionRepository.save(subscription);

        log.info("Paddle subscription mise à jour userId={} paddleSubId={} nextBilledAt={}",
                subscription.getUserId(), paddleSubscriptionId, nextBilledAt);
    }

    @SuppressWarnings("unchecked")
    private String extractCustomerId(Map<String, Object> data) {
        Object customerObj = data.get("customer_id");
        if (customerObj != null) {
            return String.valueOf(customerObj);
        }
        Map<String, Object> customer = (Map<String, Object>) data.get("customer");
        if (customer != null) {
            Object id = customer.get("id");
            return id != null ? String.valueOf(id) : null;
        }
        return null;
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Erreur parsing payload Paddle webhook : {}", e.getMessage());
            throw new PaymentException("Payload Paddle invalide", e);
        }
    }

    private void syncPlanWithAuthService(UUID userId, PlanTier plan) {
        try {
            authClient.updatePlan(PlanUpdateRequest.builder().userId(userId).plan(plan).build());
            log.info("Plan auth-service synchronisé via Paddle userId={} plan={}", userId, plan);
        } catch (Exception e) {
            log.error("Échec sync plan auth-service (Paddle) userId={} plan={} : {}",
                    userId, plan, e.getMessage());
        }
    }
}
