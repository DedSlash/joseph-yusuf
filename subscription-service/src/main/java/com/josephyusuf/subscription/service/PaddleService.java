package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PaddleConfig;
import com.josephyusuf.subscription.dto.PaddleCheckoutResponse;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Intégration Paddle Billing v2.
 * <p>
 * Pattern : transaction créée côté serveur, ouverte côté client via Paddle.js
 * (cf. https://developer.paddle.com/paddlejs/methods/paddle-checkout-open).
 * <p>
 * On NE passe PAS de {@code customer.email} ni {@code customer_email} à
 * POST /transactions : ces champs n'existent pas dans l'API Paddle. L'email
 * est collecté/pré-rempli côté Paddle.js Checkout (param {@code customer.email}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaddleService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String EARLY50 = "EARLY50";

    private final PaddleConfig paddleConfig;
    private final RestTemplate restTemplate;
    private final AdminClient adminClient;

    /**
     * Crée une transaction Paddle (POST /transactions) et retourne un
     * {@link PaddleCheckoutResponse} consommé par Paddle.js côté front.
     *
     * @param userId     id Joseph de l'utilisateur (placé dans {@code custom_data})
     * @param planTier   PREMIUM ou PREMIUM_PLUS
     * @param couponCode code promo Joseph. Le {@code paddle_discount_id}
     *                   associé est lu en DB via admin-service (table
     *                   {@code joseph_admin.promo_codes}). EARLY50 reste
     *                   supporté via fallback config si la DB est indisponible
     *                   ou ne renvoie pas la mapping (compat descendante).
     */
    @SuppressWarnings("unchecked")
    public PaddleCheckoutResponse createPayment(UUID userId, PlanTier planTier, String couponCode) {
        if (planTier == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement Paddle");
        }

        String priceId = resolvePriceId(planTier);

        Map<String, Object> item = new HashMap<>();
        item.put("price_id", priceId);
        item.put("quantity", 1);

        Map<String, Object> customData = new HashMap<>();
        customData.put("userId", userId.toString());
        customData.put("planTier", planTier.name());
        if (couponCode != null && !couponCode.isBlank()) {
            customData.put("couponCode", couponCode);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("custom_data", customData);
        body.put("collection_mode", "automatic");

        String discountId = resolveDiscountId(couponCode);
        if (discountId != null) {
            body.put("discount_id", discountId);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
        String url = paddleConfig.getBaseUrl() + "/transactions";

        ResponseEntity<Map<String, Object>> response;
        try {
            var typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
            response = restTemplate.exchange(url, HttpMethod.POST, request, typeRef);
        } catch (Exception e) {
            log.error("Erreur appel Paddle API : {}", e.getMessage());
            throw new PaymentException("Impossible de contacter Paddle. Réessayez.");
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new PaymentException("Réponse vide de Paddle");
        }

        Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
        if (data == null) {
            throw new PaymentException("Paddle : champ data absent");
        }

        String transactionId = (String) data.get("id");
        String status = (String) data.get("status");
        String checkoutUrl = extractCheckoutUrl(data);

        log.info("Paddle transaction créée userId={} plan={} txId={} status={}",
                userId, planTier, transactionId, status);

        return PaddleCheckoutResponse.builder()
                .transactionId(transactionId)
                .status(status)
                .checkoutUrl(checkoutUrl)
                .build();
    }

    /**
     * Annule une subscription Paddle existante (POST /subscriptions/{id}/cancel).
     * Effective à la fin de la période courante.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> cancelSubscription(String subscriptionId) {
        String url = paddleConfig.getBaseUrl() + "/subscriptions/" + subscriptionId + "/cancel";

        Map<String, Object> body = new HashMap<>();
        body.put("effective_from", "next_billing_period");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());
        var typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(url, HttpMethod.POST, request, typeRef);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            log.info("Paddle subscription annulée id={}", subscriptionId);
            return (Map<String, Object>) response.getBody().get("data");
        }

        log.error("Paddle cancelSubscription échoué id={} status={}",
                subscriptionId, response.getStatusCode());
        throw new PaymentException("Échec annulation subscription Paddle id=" + subscriptionId);
    }

    /**
     * Vérifie la signature du header {@code Paddle-Signature : ts=...;h1=...}.
     * <p>
     * Algorithme officiel (https://developer.paddle.com/webhooks/signature-verification) :
     * <ol>
     *   <li>extraire {@code ts} et {@code h1}</li>
     *   <li>signed payload = {@code ts + ":" + rawBody} (rawBody NON parsé)</li>
     *   <li>HMAC-SHA256 hex du signed payload avec le secret du destination notification</li>
     *   <li>comparer en timing-safe</li>
     * </ol>
     */
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Paddle webhook signature absente");
            return false;
        }

        String ts = null;
        String h1 = null;
        for (String part : signatureHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("ts=")) {
                ts = trimmed.substring(3);
            } else if (trimmed.startsWith("h1=")) {
                h1 = trimmed.substring(3);
            }
        }

        if (ts == null || h1 == null) {
            log.warn("Paddle webhook signature format invalide : {}", signatureHeader);
            return false;
        }

        String signedPayload = ts + ":" + rawPayload;
        String expected = computeHmacSha256(signedPayload, paddleConfig.getWebhookSecret());
        if (expected == null) return false;

        boolean valid = constantTimeEquals(expected, h1);
        if (!valid) {
            log.warn("Paddle webhook signature mismatch");
        }
        return valid;
    }

    private String resolvePriceId(PlanTier planTier) {
        return switch (planTier) {
            case PREMIUM -> paddleConfig.getPrices().getPremiumId();
            case PREMIUM_PLUS -> paddleConfig.getPrices().getPremiumPlusId();
            default -> throw new InvalidPlanException("Plan invalide pour Paddle: " + planTier);
        };
    }

    private String resolveDiscountId(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) return null;
        String trimmed = couponCode.trim();
        try {
            PromoCodeValidation v = adminClient.validatePublic(trimmed);
            if (v != null && v.isValid() && v.getPaddleDiscountId() != null
                    && !v.getPaddleDiscountId().isBlank()) {
                return v.getPaddleDiscountId();
            }
        } catch (Exception e) {
            log.warn("Lookup paddle discount id KO pour code={} : {}", trimmed, e.getMessage());
        }
        // Fallback EARLY50 (compat descendante avec PADDLE_EARLY50_DISCOUNT_ID).
        // À retirer une fois la migration V3 vérifiée stable en prod.
        if (EARLY50.equalsIgnoreCase(trimmed)) {
            return paddleConfig.getPrices().getEarly50DiscountId();
        }
        return null;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(paddleConfig.getApiKey());
        return headers;
    }

    @SuppressWarnings("unchecked")
    private String extractCheckoutUrl(Map<String, Object> data) {
        Object checkoutObj = data.get("checkout");
        if (checkoutObj instanceof Map<?, ?> map) {
            Object url = ((Map<String, Object>) map).get("url");
            return url == null ? null : String.valueOf(url);
        }
        return null;
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Erreur calcul HMAC-SHA256 : {}", e.getMessage());
            return null;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
