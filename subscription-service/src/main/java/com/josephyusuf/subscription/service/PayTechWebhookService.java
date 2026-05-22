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

    private final PayTechConfig config;
    private final TransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;
    private final AuthClient authClient;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Traite l'IPN PayTech : vérifie l'authenticité via SHA256(API_KEY/API_SECRET),
     * filtre sur type_event=sale_complete, active l'abonnement, synchronise le plan.
     * Idempotent : un même refCommand ne déclenche qu'une seule activation.
     */
    @Transactional
    public void handleIPN(Map<String, Object> payload) {
        String receivedKey = String.valueOf(payload.get("api_key_sha256"));
        String receivedSecret = String.valueOf(payload.get("api_secret_sha256"));
        if (!verifySignature(receivedKey, receivedSecret)) {
            throw new SecurityException("PayTech IPN signature invalide");
        }

        String typeEvent = String.valueOf(payload.get("type_event"));
        if (!"sale_complete".equals(typeEvent)) {
            log.info("PayTech IPN ignoré type_event={}", typeEvent);
            return;
        }

        String refCommand = String.valueOf(payload.get("ref_command"));
        if (isAlreadyProcessed(refCommand)) {
            log.info("PayTech IPN déjà traité ref={} — idempotence", refCommand);
            return;
        }

        Map<String, String> customField = parseCustomField(payload.get("custom_field"));
        if (customField == null) {
            log.error("PayTech IPN sans custom_field exploitable ref={}", refCommand);
            return;
        }

        String userIdStr = customField.get("userId");
        String planTier = customField.get("planTier");
        String couponCode = customField.get("couponCode");
        if (userIdStr == null || planTier == null) {
            log.error("PayTech IPN custom_field incomplet ref={} : {}", refCommand, customField);
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

    private boolean isAlreadyProcessed(String refCommand) {
        return transactionRepository.findByTransactionId(refCommand)
                .map(tx -> tx.getStatus() == TransactionStatus.SUCCEEDED)
                .orElse(false);
    }

    private Map<String, String> parseCustomField(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Map<?, ?> m) {
            Map<String, String> out = new java.util.HashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
            return out;
        }
        String s = String.valueOf(raw);
        if (s.isBlank()) return null;
        try {
            return objectMapper.readValue(s, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.error("PayTech custom_field illisible : {}", e.getMessage());
            return null;
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
