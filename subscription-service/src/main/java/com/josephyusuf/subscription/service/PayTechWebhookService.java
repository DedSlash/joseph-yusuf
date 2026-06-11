package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.dto.PlanUpdateRequest;
import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayTechWebhookService {

    /** Événements PayTech documentés (doc.intech.sn/doc_paytech.php — §IPN). */
    private static final String EVENT_SALE_COMPLETE = "sale_complete";
    private static final String EVENT_SALE_CANCELED = "sale_canceled";
    private static final String EVENT_REFUND_COMPLETE = "refund_complete";

    private final PayTechConfig config;
    private final TransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    private final AuthClient authClient;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Traite l'IPN PayTech : vérifie d'abord la signature via SHA256(API_KEY/API_SECRET),
     * puis dispatche selon {@code type_event}.
     * <p>
     * Événements gérés :
     * <ul>
     *   <li>{@code sale_complete} : active l'abonnement + sync auth-service + enregistre coupon</li>
     *   <li>{@code sale_canceled} : marque la transaction FAILED</li>
     *   <li>{@code refund_complete} : marque la transaction REFUNDED + downgrade FREE</li>
     * </ul>
     * Toutes les opérations sont idempotentes via le {@code ref_command}.
     */
    @Transactional
    public void handleIPN(Map<String, Object> payload) {
        String receivedKey = String.valueOf(payload.get("api_key_sha256"));
        String receivedSecret = String.valueOf(payload.get("api_secret_sha256"));
        if (!verifySignature(receivedKey, receivedSecret)) {
            throw new SecurityException("PayTech IPN signature invalide");
        }

        String typeEvent = String.valueOf(payload.get("type_event"));
        String refCommand = String.valueOf(payload.get("ref_command"));

        switch (typeEvent) {
            case EVENT_SALE_COMPLETE -> handleSaleComplete(refCommand, payload);
            case EVENT_SALE_CANCELED -> handleSaleCanceled(refCommand);
            case EVENT_REFUND_COMPLETE -> handleRefundComplete(refCommand, payload);
            default -> log.info("PayTech IPN ignoré type_event={} ref={}", typeEvent, refCommand);
        }
    }

    private void handleSaleComplete(String refCommand, Map<String, Object> payload) {
        if (isAlreadySucceeded(refCommand)) {
            log.info("PayTech sale_complete déjà traité ref={} — idempotence", refCommand);
            return;
        }

        Map<String, String> customField = parseCustomField(payload.get("custom_field"));
        String userIdStr = customField.get("userId");
        String planTier = customField.get("planTier");
        String couponCode = customField.get("couponCode");
        if (userIdStr == null || planTier == null) {
            log.error("PayTech sale_complete custom_field absent ou incomplet ref={} : {}",
                    refCommand, customField);
            return;
        }

        UUID userId = UUID.fromString(userIdStr);
        PlanTier plan = PlanTier.valueOf(planTier);

        subscriptionService.activateAfterPayment(userId, plan, PaymentProvider.PAYTECH, refCommand);

        if (couponCode != null && !couponCode.isBlank()) {
            try {
                adminClient.apply(PromoCodeApplyRequest.builder()
                        .code(couponCode)
                        .userId(userId)
                        .build());
            } catch (Exception e) {
                log.warn("Impossible d'enregistrer usage promo {} pour user={} : {}",
                        couponCode, userId, e.getMessage());
            }
        }

        authClient.updatePlan(PlanUpdateRequest.builder()
                .userId(userId)
                .plan(plan)
                .build());

        log.info("PayTech paiement confirmé et activé userId={} plan={} ref={}",
                userId, planTier, refCommand);
    }

    private void handleSaleCanceled(String refCommand) {
        if (isAlreadyTerminal(refCommand)) {
            log.info("PayTech sale_canceled ignoré, tx déjà terminale ref={}", refCommand);
            return;
        }
        subscriptionService.markTransactionFailed(refCommand, "Annulé par l'utilisateur sur PayTech");
        log.info("PayTech sale_canceled traité ref={}", refCommand);
    }

    private void handleRefundComplete(String refCommand, Map<String, Object> payload) {
        if (isAlreadyRefunded(refCommand)) {
            log.info("PayTech refund_complete déjà traité ref={} — idempotence", refCommand);
            return;
        }

        Map<String, String> customField = parseCustomField(payload.get("custom_field"));
        String userIdStr = customField.get("userId");
        if (userIdStr == null) {
            log.error("PayTech refund_complete sans userId dans custom_field ref={}", refCommand);
            subscriptionService.markTransactionRefunded(refCommand);
            return;
        }

        UUID userId = UUID.fromString(userIdStr);
        subscriptionService.markRefundAndDowngrade(userId, refCommand);
        log.info("PayTech refund traité userId={} ref={}", userId, refCommand);
    }

    private boolean isAlreadySucceeded(String refCommand) {
        return transactionRepository.findByTransactionId(refCommand)
                .map(tx -> tx.getStatus() == TransactionStatus.SUCCEEDED)
                .orElse(false);
    }

    private boolean isAlreadyRefunded(String refCommand) {
        return transactionRepository.findByTransactionId(refCommand)
                .map(tx -> tx.getStatus() == TransactionStatus.REFUNDED)
                .orElse(false);
    }

    private boolean isAlreadyTerminal(String refCommand) {
        return transactionRepository.findByTransactionId(refCommand)
                .map(tx -> tx.getStatus() == TransactionStatus.SUCCEEDED
                        || tx.getStatus() == TransactionStatus.FAILED
                        || tx.getStatus() == TransactionStatus.CANCELLED
                        || tx.getStatus() == TransactionStatus.REFUNDED)
                .orElse(false);
    }

    private Map<String, String> parseCustomField(Object raw) {
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> m) {
            Map<String, String> out = new java.util.HashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
            return out;
        }
        String s = String.valueOf(raw);
        if (s.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(s, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.error("PayTech custom_field illisible : {}", e.getMessage());
            return Map.of();
        }
    }

    private boolean verifySignature(String receivedKey, String receivedSecret) {
        try {
            String expectedKey = sha256Hex(config.getApiKey());
            String expectedSecret = sha256Hex(config.getApiSecret());
            return expectedKey.equalsIgnoreCase(receivedKey)
                    && expectedSecret.equalsIgnoreCase(receivedSecret);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 indisponible : {}", e.getMessage());
            return false;
        }
    }

    private String sha256Hex(String value) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }
}
