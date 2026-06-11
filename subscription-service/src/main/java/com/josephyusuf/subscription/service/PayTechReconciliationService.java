package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.dto.PlanUpdateRequest;
import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Rattrape une transaction PayTech PENDING dont l'IPN a été perdue.
 * Appelle {@code GET /payment/get-status?token_payment=...} puis dispatche
 * le résultat dans le même pipeline que le webhook (activateAfterPayment +
 * sync auth + apply coupon, ou markTransactionFailed).
 *
 * Doc : https://doc.intech.sn/doc_paytech.php — §GET PAYMENT STATUS
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayTechReconciliationService {

    /** Valeurs de {@code type_event} renvoyées par get-status (mêmes que l'IPN). */
    private static final String EVENT_SALE_COMPLETE = "sale_complete";
    private static final String EVENT_SALE_CANCELED = "sale_canceled";
    private static final String EVENT_SALE_PENDING = "sale_pending";

    private final PayTechConfig config;
    private final RestTemplate restTemplate;
    private final SubscriptionService subscriptionService;
    private final AuthClient authClient;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    public void reconcile(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new PaymentException(
                    "Réconciliation possible uniquement sur transactions PENDING (statut actuel : "
                            + transaction.getStatus() + ")");
        }
        if (!isPayTechProvider(transaction.getProvider())) {
            throw new PaymentException(
                    "Provider " + transaction.getProvider() + " n'est pas réconciliable via PayTech");
        }
        if (transaction.getProviderToken() == null || transaction.getProviderToken().isBlank()) {
            throw new PaymentException(
                    "Token PayTech absent (transaction antérieure à la migration). Utiliser force-activate manuellement.");
        }

        Map<String, Object> body = callGetStatus(transaction.getProviderToken(), transaction.getTransactionId());
        if (!"1".equals(String.valueOf(body.get("success")))) {
            String message = String.valueOf(body.getOrDefault("message", "erreur inconnue"));
            throw new PaymentException("PayTech get-status a échoué : " + message);
        }

        String typeEvent = String.valueOf(body.getOrDefault("type_event", ""));
        switch (typeEvent) {
            case EVENT_SALE_COMPLETE ->
                    applySaleComplete(transaction, body);
            case EVENT_SALE_CANCELED ->
                    applySaleCanceled(transaction.getTransactionId());
            case EVENT_SALE_PENDING -> log.info(
                    "Réconciliation PayTech ref={} toujours PENDING côté PayTech — aucune action",
                    transaction.getTransactionId());
            default -> log.warn(
                    "Réconciliation PayTech ref={} type_event inattendu='{}' — aucune action",
                    transaction.getTransactionId(), typeEvent);
        }
    }

    private Map<String, Object> callGetStatus(String token, String refCommand) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("API_KEY", config.getApiKey());
        headers.set("API_SECRET", config.getApiSecret());

        String url = config.getBaseUrl() + "/payment/get-status?token_payment=" + token;
        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
        } catch (Exception e) {
            log.error("PayTech /get-status erreur réseau ref={} : {}", refCommand, e.getMessage());
            throw new PaymentException("Impossible de contacter PayTech pour la réconciliation. Réessayez.");
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new PaymentException("Réponse PayTech /get-status vide");
        }
        return body;
    }

    private void applySaleComplete(Transaction transaction, Map<String, Object> body) {
        UUID userId = transaction.getUserId();
        PlanTier plan = transaction.getPlan();
        String refCommand = transaction.getTransactionId();

        subscriptionService.activateAfterPayment(userId, plan, PaymentProvider.PAYTECH, refCommand);

        String couponCode = parseCustomField(body.get("custom_field")).get("couponCode");
        if (couponCode == null || couponCode.isBlank()) {
            couponCode = transaction.getPromoCode();
        }
        if (couponCode != null && !couponCode.isBlank()) {
            try {
                adminClient.apply(PromoCodeApplyRequest.builder()
                        .code(couponCode)
                        .userId(userId)
                        .build());
            } catch (Exception e) {
                log.warn("Réconciliation : usage promo {} non enregistré pour user={} : {}",
                        couponCode, userId, e.getMessage());
            }
        }

        authClient.updatePlan(PlanUpdateRequest.builder()
                .userId(userId)
                .plan(plan)
                .build());

        log.info("Réconciliation PayTech : tx {} activée pour userId={} plan={}",
                refCommand, userId, plan);
    }

    private void applySaleCanceled(String refCommand) {
        subscriptionService.markTransactionFailed(refCommand, "Annulé par l'utilisateur sur PayTech (réconciliation)");
        log.info("Réconciliation PayTech : tx {} marquée FAILED", refCommand);
    }

    private boolean isPayTechProvider(PaymentProvider provider) {
        return provider == PaymentProvider.PAYTECH
                || provider == PaymentProvider.WAVE
                || provider == PaymentProvider.ORANGE_MONEY
                || provider == PaymentProvider.FREE_MONEY
                || provider == PaymentProvider.CARTE;
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
            log.warn("Réconciliation : custom_field illisible : {}", e.getMessage());
            return Map.of();
        }
    }
}
