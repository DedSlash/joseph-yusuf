package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PaddleConfig;
import com.josephyusuf.subscription.dto.PaddleCheckoutResponse;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaddleServiceTest {

    private static final String WEBHOOK_SECRET = "pdl_ntfset_test_secret_value";

    @Mock private RestTemplate restTemplate;
    @Mock private AdminClient adminClient;
    @Mock private SubscriptionService subscriptionService;

    private PaddleConfig config;
    private PaddleService paddleService;

    @BeforeEach
    void setUp() {
        config = new PaddleConfig();
        config.setApiKey("apikey_test");
        config.setClientToken("client_token_test");
        config.setWebhookSecret(WEBHOOK_SECRET);
        config.setSandbox(true);
        PaddleConfig.Prices prices = new PaddleConfig.Prices();
        prices.setPremiumId("pri_premium_test");
        prices.setPremiumPlusId("pri_premium_plus_test");
        prices.setEarly50DiscountId("dsc_early50_test");
        config.setPrices(prices);

        paddleService = new PaddleService(config, restTemplate, adminClient, subscriptionService);
    }

    private static PromoCodeValidation validWith(String paddleDiscountId) {
        return PromoCodeValidation.builder()
                .valid(true)
                .paddleDiscountId(paddleDiscountId)
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPaddleOk(String txId, String checkoutUrl) {
        stubPaddleOkWithTotals(txId, checkoutUrl, null, null, null);
    }

    /** Stub avec totals + currency comme la vraie API Paddle les renvoie. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPaddleOkWithTotals(String txId, String checkoutUrl,
                                        String totalCents, String subtotalCents,
                                        String currencyCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", txId);
        data.put("status", "ready");
        if (checkoutUrl != null) {
            data.put("checkout", Map.of("url", checkoutUrl));
        }
        if (currencyCode != null) {
            data.put("currency_code", currencyCode);
        }
        if (totalCents != null || subtotalCents != null) {
            Map<String, Object> totals = new HashMap<>();
            if (totalCents != null) totals.put("total", totalCents);
            if (subtotalCents != null) totals.put("subtotal", subtotalCents);
            Map<String, Object> details = new HashMap<>();
            details.put("totals", totals);
            data.put("details", details);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("data", data);

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST),
                any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn((ResponseEntity) new ResponseEntity<>(body, HttpStatus.OK));
    }

    @Test
    @DisplayName("Plan FREE → InvalidPlanException, pas d'appel HTTP")
    void createPayment_free_throws() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> paddleService.createPayment(userId, PlanTier.FREE, null))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("Sandbox → baseUrl sandbox-api.paddle.com")
    void baseUrl_sandbox() {
        assertThat(config.getBaseUrl()).isEqualTo("https://sandbox-api.paddle.com");
    }

    @Test
    @DisplayName("Production → baseUrl api.paddle.com")
    void baseUrl_production() {
        config.setSandbox(false);
        assertThat(config.getBaseUrl()).isEqualTo("https://api.paddle.com");
    }

    @Test
    @DisplayName("PREMIUM sans coupon → body items=[premium price], pas de discount_id")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_premium_noCoupon() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_abc", "https://pay.paddle.com/abc");

        PaddleCheckoutResponse res = paddleService.createPayment(userId, PlanTier.PREMIUM, null);

        assertThat(res.getTransactionId()).isEqualTo("txn_abc");
        assertThat(res.getStatus()).isEqualTo("ready");
        assertThat(res.getCheckoutUrl()).isEqualTo("https://pay.paddle.com/abc");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(eq("https://sandbox-api.paddle.com/transactions"),
                eq(HttpMethod.POST), captor.capture(), any(ParameterizedTypeReference.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body)
                .doesNotContainKeys("discount_id", "customer_email", "customer")
                .containsEntry("collection_mode", "automatic");

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0))
                .containsEntry("price_id", "pri_premium_test")
                .containsEntry("quantity", 1);

        Map<String, Object> customData = (Map<String, Object>) body.get("custom_data");
        assertThat(customData)
                .containsEntry("userId", userId.toString())
                .containsEntry("planTier", "PREMIUM")
                .doesNotContainKey("couponCode");
    }

    @Test
    @DisplayName("Code promo + adminClient renvoie paddleDiscountId → utilisé dans body Paddle")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_couponFromDb_usesDbDiscountId() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_def", null);
        when(adminClient.validatePublic("JOSEPH20")).thenReturn(validWith("dsc_from_db"));

        paddleService.createPayment(userId, PlanTier.PREMIUM_PLUS, "JOSEPH20");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST),
                captor.capture(), any(ParameterizedTypeReference.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).containsEntry("discount_id", "dsc_from_db");

        Map<String, Object> customData = (Map<String, Object>) body.get("custom_data");
        assertThat(customData).containsEntry("couponCode", "JOSEPH20");
    }

    @Test
    @DisplayName("EARLY50 + adminClient KO → fallback config.early50DiscountId (compat)")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_early50_adminDown_fallsBackToConfig() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_def", null);
        when(adminClient.validatePublic("EARLY50")).thenThrow(new RuntimeException("admin-service down"));

        paddleService.createPayment(userId, PlanTier.PREMIUM_PLUS, "EARLY50");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST),
                captor.capture(), any(ParameterizedTypeReference.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).containsEntry("discount_id", "dsc_early50_test");
    }

    @Test
    @DisplayName("Code non-EARLY50 sans paddleDiscountId en DB → pas de discount_id, couponCode tracké")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_couponWithoutPaddleId_noDiscountId() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_x", null);
        when(adminClient.validatePublic("SOMEOTHER")).thenReturn(validWith(null));

        paddleService.createPayment(userId, PlanTier.PREMIUM, "SOMEOTHER");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST),
                captor.capture(), any(ParameterizedTypeReference.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).doesNotContainKey("discount_id");
        Map<String, Object> customData = (Map<String, Object>) body.get("custom_data");
        assertThat(customData).containsEntry("couponCode", "SOMEOTHER");
    }

    @Test
    @DisplayName("Paddle ne renvoie pas de totals → fallback monthlyEur, currency EUR par défaut")
    void createPayment_recordsPendingTransaction_premium_fallback() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_abc", null);

        paddleService.createPayment(userId, PlanTier.PREMIUM, null);

        ArgumentCaptor<com.josephyusuf.subscription.dto.PendingTransactionParams> captor =
                ArgumentCaptor.forClass(com.josephyusuf.subscription.dto.PendingTransactionParams.class);
        org.mockito.Mockito.verify(subscriptionService).recordPendingTransaction(captor.capture());

        com.josephyusuf.subscription.dto.PendingTransactionParams params = captor.getValue();
        assertThat(params.getProvider()).isEqualTo(com.josephyusuf.subscription.enums.PaymentProvider.PADDLE);
        assertThat(params.getExternalTxId()).isEqualTo("txn_abc");
        assertThat(params.getAmount()).isEqualByComparingTo(new java.math.BigDecimal("4.99"));
        assertThat(params.getCurrency()).isEqualTo("EUR");
        assertThat(params.getMonthsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Paddle renvoie totals USD avec discount → on enregistre le montant réel facturé")
    void createPayment_recordsPendingTransaction_fromPaddleTotals() {
        UUID userId = UUID.randomUUID();
        // 9.99 USD remisé à 2.00 USD (cas réel DEBIT299 capé par minimum Paddle)
        stubPaddleOkWithTotals("txn_xyz", null, "200", "999", "USD");
        when(adminClient.validatePublic("DEBIT299")).thenReturn(
                PromoCodeValidation.builder()
                        .valid(true).code("DEBIT299").discountPercent(99)
                        .paddleDiscountId("dsc_real").lifetime(false).build());

        paddleService.createPayment(userId, PlanTier.PREMIUM_PLUS, "DEBIT299");

        ArgumentCaptor<com.josephyusuf.subscription.dto.PendingTransactionParams> captor =
                ArgumentCaptor.forClass(com.josephyusuf.subscription.dto.PendingTransactionParams.class);
        org.mockito.Mockito.verify(subscriptionService).recordPendingTransaction(captor.capture());

        com.josephyusuf.subscription.dto.PendingTransactionParams params = captor.getValue();
        assertThat(params.getAmount()).isEqualByComparingTo(new java.math.BigDecimal("2.00"));
        assertThat(params.getOriginalAmount()).isEqualByComparingTo(new java.math.BigDecimal("9.99"));
        assertThat(params.getCurrency()).isEqualTo("USD");
        // discountPercent vient d'admin-service (vue Joseph du promo), pas du montant Paddle
        assertThat(params.getDiscountPercent()).isEqualTo(99);
        assertThat(params.getPromoCode()).isEqualTo("DEBIT299");
        assertThat(params.isCouponLifetime()).isFalse();
    }

    @Test
    @DisplayName("Erreur HTTP Paddle → PaymentException")
    void createPayment_apiError_throws() {
        UUID userId = UUID.randomUUID();
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST),
                any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("network down"));

        assertThatThrownBy(() -> paddleService.createPayment(userId, PlanTier.PREMIUM, null))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Paddle");
    }

    @Test
    @DisplayName("Réponse sans data → PaymentException")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_noData_throws() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST),
                any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn((ResponseEntity) new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> paddleService.createPayment(userId, PlanTier.PREMIUM, null))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("data");
    }

    @Test
    @DisplayName("verifyWebhookSignature : ts + h1 valides → true")
    void verifySig_ok() throws Exception {
        long ts = 1717000000L;
        String body = "{\"event_id\":\"evt_x\",\"event_type\":\"transaction.completed\"}";
        String h1 = hmac(WEBHOOK_SECRET, ts + ":" + body);
        String header = "ts=" + ts + ";h1=" + h1;

        assertThat(paddleService.verifyWebhookSignature(body, header)).isTrue();
    }

    @Test
    @DisplayName("verifyWebhookSignature : h1 falsifié → false")
    void verifySig_tampered_fail() {
        long ts = 1717000000L;
        String body = "{\"event_id\":\"evt_x\"}";
        String header = "ts=" + ts + ";h1=deadbeef0000000000000000000000000000000000000000000000000000beef";

        assertThat(paddleService.verifyWebhookSignature(body, header)).isFalse();
    }

    @Test
    @DisplayName("verifyWebhookSignature : header null ou vide → false")
    void verifySig_missing_header_fail() {
        assertThat(paddleService.verifyWebhookSignature("{}", null)).isFalse();
        assertThat(paddleService.verifyWebhookSignature("{}", "")).isFalse();
    }

    @Test
    @DisplayName("verifyWebhookSignature : header sans h1 → false")
    void verifySig_no_h1_fail() {
        assertThat(paddleService.verifyWebhookSignature("{}", "ts=12345")).isFalse();
    }

    private static String hmac(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
