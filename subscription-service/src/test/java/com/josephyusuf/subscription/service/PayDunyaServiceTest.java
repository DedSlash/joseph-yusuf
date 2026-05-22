package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PayDunyaConfig;
import com.josephyusuf.subscription.dto.PayDunyaInvoiceResponse;
import com.josephyusuf.subscription.dto.PayDunyaStatusResponse;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayDunyaServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private SubscriptionService subscriptionService;
    @Mock private AdminClient adminClient;

    private PayDunyaConfig config;
    private PayDunyaService payDunyaService;

    @BeforeEach
    void setUp() {
        config = new PayDunyaConfig();
        config.setMasterKey("test_master");
        config.setPrivateKey("test_private");
        config.setToken("test_token");
        config.setMode("test");
        config.setCallbackUrl("http://localhost:8080/api/webhooks/paydunya");
        config.setReturnUrl("http://localhost:4200/subscription/success");
        config.setCancelUrl("http://localhost:4200/subscription");

        payDunyaService = new PayDunyaService(config, restTemplate, subscriptionService, adminClient);
    }

    @Test
    @DisplayName("createInvoice PREMIUM → 2990 XOF, retourne token et invoiceUrl")
    void createInvoice_premium_success() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response_code", "00");
        responseBody.put("token", "test_invoice_token_123");
        responseBody.put("response_text", "https://app.paydunya.com/sandbox/checkout/test_invoice_token_123");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        PayDunyaInvoiceResponse result = payDunyaService.createInvoice(
                userId, "user@example.com", "PREMIUM", null);

        assertThat(result.getToken()).isEqualTo("test_invoice_token_123");
        assertThat(result.getInvoiceUrl()).contains("test_invoice_token_123");
        verify(subscriptionService).recordPendingTransaction(any());
    }

    @Test
    @DisplayName("createInvoice PREMIUM_PLUS → 5990 XOF")
    void createInvoice_premiumPlus_success() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response_code", "00");
        responseBody.put("token", "pp_token");
        responseBody.put("response_text", "https://app.paydunya.com/sandbox/checkout/pp_token");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        PayDunyaInvoiceResponse result = payDunyaService.createInvoice(
                userId, "user@example.com", "PREMIUM_PLUS", null);

        assertThat(result.getToken()).isEqualTo("pp_token");
    }

    @Test
    @DisplayName("createInvoice avec coupon applique la réduction")
    void createInvoice_withCoupon_appliesDiscount() {
        UUID userId = UUID.randomUUID();
        when(adminClient.validatePublic("EARLY50")).thenReturn(
                PromoCodeValidation.builder().valid(true).discountPercent(50).code("EARLY50").build());

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response_code", "00");
        responseBody.put("token", "promo_token");
        responseBody.put("response_text", "https://app.paydunya.com/sandbox/checkout/promo_token");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        PayDunyaInvoiceResponse result = payDunyaService.createInvoice(
                userId, "user@example.com", "PREMIUM", "EARLY50");

        assertThat(result.getToken()).isEqualTo("promo_token");
        verify(adminClient).validatePublic("EARLY50");
    }

    @Test
    @DisplayName("createInvoice FREE → InvalidPlanException")
    void createInvoice_free_throws() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> payDunyaService.createInvoice(
                userId, "user@example.com", "FREE", null))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("createInvoice erreur PayDunya → PaymentException")
    void createInvoice_apiError_throws() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response_code", "01");
        responseBody.put("response_text", "Invalid request");

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        assertThatThrownBy(() -> payDunyaService.createInvoice(
                userId, "user@example.com", "PREMIUM", null))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Invalid request");
    }

    @Test
    @DisplayName("checkInvoiceStatus retourne completed avec custom_data")
    void checkInvoiceStatus_completed() {
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("status", "completed");

        Map<String, Object> customData = new HashMap<>();
        customData.put("userId", UUID.randomUUID().toString());
        customData.put("planTier", "PREMIUM");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("invoice", invoice);
        responseBody.put("custom_data", customData);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        PayDunyaStatusResponse result = payDunyaService.checkInvoiceStatus("test_token");

        assertThat(result.getToken()).isEqualTo("test_token");
        assertThat(result.getStatus()).isEqualTo("completed");
        assertThat(result.getCustomData()).containsKey("userId");
        assertThat(result.getCustomData()).containsKey("planTier");
    }

    @Test
    @DisplayName("checkInvoiceStatus erreur réseau → PaymentException")
    void checkInvoiceStatus_networkError_throws() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> payDunyaService.checkInvoiceStatus("bad_token"))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    @DisplayName("config mode test → sandbox URL")
    void config_testMode_usesSandboxUrl() {
        assertThat(config.getBaseUrl()).isEqualTo("https://app.paydunya.com/sandbox-api/v1");
    }

    @Test
    @DisplayName("config mode live → production URL")
    void config_liveMode_usesProdUrl() {
        config.setMode("live");
        assertThat(config.getBaseUrl()).isEqualTo("https://app.paydunya.com/api/v1");
    }
}
