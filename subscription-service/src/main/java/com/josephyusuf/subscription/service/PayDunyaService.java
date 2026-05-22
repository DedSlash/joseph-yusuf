package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PayDunyaConfig;
import com.josephyusuf.subscription.dto.PayDunyaInvoiceResponse;
import com.josephyusuf.subscription.dto.PayDunyaStatusResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayDunyaService {

    private final PayDunyaConfig config;
    private final RestTemplate restTemplate;
    private final SubscriptionService subscriptionService;
    private final AdminClient adminClient;

    public PayDunyaInvoiceResponse createInvoice(UUID userId, String email,
                                                  String planTier, String couponCode) {
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

        Map<String, Object> invoiceData = buildInvoicePayload(
                userId, planTier, couponCode, amount, email);

        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(invoiceData, headers);

        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/checkout-invoice/create",
                    request, (Class<Map<String, Object>>) (Class<?>) Map.class);
        } catch (Exception e) {
            log.error("Erreur appel PayDunya API : {}", e.getMessage());
            throw new PaymentException("Impossible de contacter PayDunya. Réessayez.");
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new PaymentException("Réponse vide de PayDunya");
        }

        String responseCode = String.valueOf(body.get("response_code"));
        if (!"00".equals(responseCode)) {
            throw new PaymentException("Erreur PayDunya : " + body.get("response_text"));
        }

        String token = String.valueOf(body.get("token"));

        subscriptionService.recordPendingTransaction(PendingTransactionParams.builder()
                .userId(userId)
                .plan(plan)
                .provider(PaymentProvider.PAYDUNYA)
                .externalTxId(token)
                .amount(amount)
                .currency("XOF")
                .promoCode(couponCode)
                .discountPercent(discountPercent)
                .originalAmount(discountPercent != null ? originalAmount : null)
                .build());

        String invoiceUrl = extractInvoiceUrl(body);

        log.info("PayDunya invoice créée userId={} plan={} amount={} XOF token={}",
                userId, planTier, amount, token);

        return PayDunyaInvoiceResponse.builder()
                .token(token)
                .invoiceUrl(invoiceUrl)
                .build();
    }

    @SuppressWarnings("unchecked")
    public PayDunyaStatusResponse checkInvoiceStatus(String token) {
        HttpHeaders headers = buildHeaders();
        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(
                    config.getBaseUrl() + "/checkout-invoice/confirm/" + token,
                    HttpMethod.GET, request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
        } catch (Exception e) {
            log.error("Erreur vérification statut PayDunya token={} : {}", token, e.getMessage());
            throw new PaymentException("Impossible de vérifier le statut du paiement");
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new PaymentException("Réponse vide de PayDunya");
        }

        String status = "pending";
        Object invoiceObj = body.get("invoice");
        if (invoiceObj instanceof Map) {
            Object statusObj = ((Map<String, Object>) invoiceObj).get("status");
            if (statusObj != null) {
                status = String.valueOf(statusObj);
            }
        }

        Map<String, Object> customData = null;
        Object customObj = body.get("custom_data");
        if (customObj instanceof Map) {
            customData = (Map<String, Object>) customObj;
        }

        return PayDunyaStatusResponse.builder()
                .token(token)
                .status(status)
                .customData(customData)
                .build();
    }

    private BigDecimal resolvePlanAmount(PlanTier plan) {
        return switch (plan) {
            case PREMIUM -> new BigDecimal("2990");
            case PREMIUM_PLUS -> new BigDecimal("5990");
            default -> throw new InvalidPlanException("Plan invalide pour PayDunya: " + plan);
        };
    }

    private String extractInvoiceUrl(Map<String, Object> body) {
        Object responseText = body.get("response_text");
        if (responseText != null && String.valueOf(responseText).startsWith("http")) {
            return String.valueOf(responseText);
        }
        Object tokenObj = body.get("token");
        return config.getBaseUrl().replace("/api/v1", "")
                .replace("/sandbox-api/v1", "")
                + "/checkout/invoice/" + tokenObj;
    }

    private Map<String, Object> buildInvoicePayload(UUID userId, String planTier,
                                                     String couponCode, BigDecimal amount,
                                                     String email) {
        Map<String, Object> invoiceData = new HashMap<>();

        Map<String, Object> invoice = new HashMap<>();
        invoice.put("total_amount", amount.intValue());
        invoice.put("description", "Abonnement Joseph·Yusuf " + planTier + " - 1 mois");
        invoiceData.put("invoice", invoice);

        Map<String, Object> store = new HashMap<>();
        store.put("name", "Joseph·Yusuf");
        store.put("tagline", "Gérez vos revenus avec la sagesse de Joseph");
        store.put("website_url", "https://josephyusuf.com");
        invoiceData.put("store", store);

        Map<String, Object> actions = new HashMap<>();
        actions.put("callback_url", config.getCallbackUrl());
        actions.put("return_url", config.getReturnUrl() + "?plan=" + planTier);
        actions.put("cancel_url", config.getCancelUrl());
        invoiceData.put("actions", actions);

        Map<String, Object> customData = new HashMap<>();
        customData.put("userId", userId.toString());
        customData.put("planTier", planTier);
        customData.put("email", email);
        if (couponCode != null) {
            customData.put("couponCode", couponCode);
        }
        invoiceData.put("custom_data", customData);

        return invoiceData;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("PAYDUNYA-MASTER-KEY", config.getMasterKey());
        headers.set("PAYDUNYA-PRIVATE-KEY", config.getPrivateKey());
        headers.set("PAYDUNYA-TOKEN", config.getToken());
        return headers;
    }
}
