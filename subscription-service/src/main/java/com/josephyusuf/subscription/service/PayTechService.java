package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.dto.PayTechPaymentResponse;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayTechService {

    private final PayTechConfig config;
    private final RestTemplate restTemplate;
    private final SubscriptionService subscriptionService;
    private final AdminClient adminClient;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public PayTechPaymentResponse createPayment(UUID userId, String planTier, String couponCode,
                                                String paytechMethodCode) {
        PlanTier plan = PlanTier.valueOf(planTier);
        if (plan == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement");
        }

        BigDecimal amount = resolvePlanAmount(plan);
        BigDecimal originalAmount = amount;
        Integer discountPercent = null;

        if (couponCode != null && !couponCode.isBlank()) {
            PromoCodeValidation validation = adminClient.validatePublic(couponCode);
            if (validation.isValid()) {
                discountPercent = validation.getDiscountPercent();
                amount = amount.multiply(BigDecimal.valueOf(100L - discountPercent))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            }
        }

        String refCommand = "JY-" + userId.toString().substring(0, 8)
                + "-" + System.currentTimeMillis();

        Map<String, Object> params = buildPaymentPayload(userId, planTier, couponCode,
                amount, refCommand, paytechMethodCode);
        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/payment/request-payment",
                    request, (Class<Map<String, Object>>) (Class<?>) Map.class);
        } catch (Exception e) {
            log.error("Erreur appel PayTech API : {}", e.getMessage());
            throw new PaymentException("Impossible de contacter PayTech. Réessayez.");
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new PaymentException("Réponse vide de PayTech");
        }

        if (!"1".equals(String.valueOf(body.get("success")))) {
            throw new PaymentException("Erreur PayTech : " + body.get("message"));
        }

        Map<String, String> redirectUrls = extractRedirectUrls(body);

        subscriptionService.recordPendingTransaction(PendingTransactionParams.builder()
                .userId(userId)
                .plan(plan)
                .provider(providerFromMethodCode(paytechMethodCode))
                .externalTxId(refCommand)
                .amount(amount)
                .currency("XOF")
                .promoCode(couponCode)
                .discountPercent(discountPercent)
                .originalAmount(discountPercent != null ? originalAmount : null)
                .build());

        log.info("PayTech paiement créé userId={} plan={} method={} amount={} XOF ref={}",
                userId, planTier, paytechMethodCode, amount, refCommand);

        return PayTechPaymentResponse.builder()
                .refCommand(refCommand)
                .redirectUrl(redirectUrls.get("regular"))
                .mobileRedirectUrl(redirectUrls.get("express"))
                .build();
    }

    private BigDecimal resolvePlanAmount(PlanTier plan) {
        return switch (plan) {
            case PREMIUM -> new BigDecimal("2990");
            case PREMIUM_PLUS -> new BigDecimal("5990");
            default -> throw new InvalidPlanException("Plan invalide pour PayTech: " + plan);
        };
    }

    /**
     * Mapping inverse code PayTech → PaymentProvider pour tracer le moyen réellement choisi.
     * Les codes attendus sont littéralement ceux de la doc officielle (§TARGET PAYMENT LIST) :
     * "Wave", "Orange Money", "Free Money", "Carte Bancaire".
     * Si l'utilisateur n'a pas pré-sélectionné de moyen, on retombe sur PAYTECH (agrégateur).
     */
    private PaymentProvider providerFromMethodCode(String paytechMethodCode) {
        if (paytechMethodCode == null) return PaymentProvider.PAYTECH;
        return switch (paytechMethodCode) {
            case "Wave" -> PaymentProvider.WAVE;
            case "Orange Money" -> PaymentProvider.ORANGE_MONEY;
            case "Free Money" -> PaymentProvider.FREE_MONEY;
            case "Carte Bancaire" -> PaymentProvider.CARTE;
            default -> PaymentProvider.PAYTECH;
        };
    }

    private Map<String, Object> buildPaymentPayload(UUID userId, String planTier, String couponCode,
                                                    BigDecimal amount, String refCommand,
                                                    String paytechMethodCode) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("item_name", "Abonnement Joseph·Yusuf " + planTier);
        params.put("item_price", amount.intValue());
        params.put("currency", "XOF");
        params.put("ref_command", refCommand);
        params.put("command_name", "Abonnement " + planTier + " - Joseph·Yusuf");
        params.put("env", config.getEnv());
        params.put("ipn_url", config.getIpnUrl());
        params.put("success_url", config.getSuccessUrl() + "?ref=" + refCommand);
        params.put("cancel_url", config.getCancelUrl());

        if (paytechMethodCode != null && !paytechMethodCode.isBlank()) {
            params.put("target_payment", paytechMethodCode);
        }

        Map<String, String> customField = new HashMap<>();
        customField.put("userId", userId.toString());
        customField.put("planTier", planTier);
        customField.put("couponCode", couponCode != null ? couponCode : "");
        if (paytechMethodCode != null) {
            customField.put("paytechMethodCode", paytechMethodCode);
        }
        try {
            params.put("custom_field", objectMapper.writeValueAsString(customField));
        } catch (JsonProcessingException e) {
            throw new PaymentException("Impossible de sérialiser custom_field PayTech", e);
        }
        return params;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("API_KEY", config.getApiKey());
        headers.set("API_SECRET", config.getApiSecret());
        return headers;
    }

    private Map<String, String> extractRedirectUrls(Map<String, Object> body) {
        // PayTech renvoie soit redirect_url:{regular,express} (objet), soit redirect_url:"https://..." (string)
        Object redirectObj = body.get("redirect_url");
        if (redirectObj instanceof Map<?, ?> map) {
            Map<String, String> out = new HashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), String.valueOf(v)));
            return out;
        }
        String single = redirectObj != null ? String.valueOf(redirectObj) : null;
        Map<String, String> fallback = new HashMap<>();
        fallback.put("regular", single);
        fallback.put("express", single);
        return fallback;
    }
}
