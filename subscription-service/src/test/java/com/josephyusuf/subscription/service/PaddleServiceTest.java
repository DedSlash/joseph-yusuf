package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.config.PaddleConfig;
import com.josephyusuf.subscription.dto.PaddleCheckoutResponse;
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

        paddleService = new PaddleService(config, restTemplate);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPaddleOk(String txId, String checkoutUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", txId);
        data.put("status", "ready");
        if (checkoutUrl != null) {
            data.put("checkout", Map.of("url", checkoutUrl));
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
    @DisplayName("PREMIUM_PLUS + couponCode EARLY50 → discount_id Paddle + couponCode dans custom_data")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_premiumPlus_earlyCoupon() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_def", null);

        paddleService.createPayment(userId, PlanTier.PREMIUM_PLUS, "EARLY50");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST),
                captor.capture(), any(ParameterizedTypeReference.class));

        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertThat(body).containsEntry("discount_id", "dsc_early50_test");

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items.get(0)).containsEntry("price_id", "pri_premium_plus_test");

        Map<String, Object> customData = (Map<String, Object>) body.get("custom_data");
        assertThat(customData).containsEntry("couponCode", "EARLY50");
    }

    @Test
    @DisplayName("Coupon non-EARLY50 → ignoré côté Paddle, gardé dans custom_data pour tracking")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void createPayment_unknownCoupon_noDiscountId() {
        UUID userId = UUID.randomUUID();
        stubPaddleOk("txn_x", null);

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
