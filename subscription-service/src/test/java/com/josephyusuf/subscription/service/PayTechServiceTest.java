package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.dto.PayTechPaymentResponse;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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
class PayTechServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private SubscriptionService subscriptionService;
    @Mock private AdminClient adminClient;

    private PayTechConfig config;
    private PayTechService payTechService;

    @BeforeEach
    void setUp() {
        config = new PayTechConfig();
        config.setApiKey("test_api_key");
        config.setApiSecret("test_api_secret");
        config.setEnv("test");
        config.setSuccessUrl("https://josephyusuf.com/subscription/success");
        config.setCancelUrl("https://josephyusuf.com/subscription");
        config.setIpnUrl("https://api.josephyusuf.com/api/webhooks/paytech");

        payTechService = new PayTechService(config, restTemplate, subscriptionService,
                adminClient, new ObjectMapper());
    }

    @Test
    @DisplayName("createPayment PREMIUM → 2990 XOF + ref JY-* + redirect_url régulier")
    @SuppressWarnings("unchecked")
    void createPayment_premium_success() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> redirect = new HashMap<>();
        redirect.put("regular", "https://paytech.sn/payment/checkout/abc");
        redirect.put("express", "https://paytech.sn/payment/mobile/abc");
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("redirect_url", redirect);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        PayTechPaymentResponse result = payTechService.createPayment(userId, "PREMIUM", null);

        assertThat(result.getRefCommand()).startsWith("JY-");
        assertThat(result.getRedirectUrl()).isEqualTo("https://paytech.sn/payment/checkout/abc");
        assertThat(result.getMobileRedirectUrl()).isEqualTo("https://paytech.sn/payment/mobile/abc");

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("2990"));
        assertThat(captor.getValue().getCurrency()).isEqualTo("XOF");
    }

    @Test
    @DisplayName("createPayment PREMIUM_PLUS → 5990 XOF")
    @SuppressWarnings("unchecked")
    void createPayment_premiumPlus_success() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("success", "1");
        body.put("redirect_url", Map.of("regular", "u", "express", "m"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        payTechService.createPayment(userId, "PREMIUM_PLUS", null);

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("5990"));
    }

    @Test
    @DisplayName("createPayment avec EARLY50 → 1495 XOF (50% sur 2990)")
    @SuppressWarnings("unchecked")
    void createPayment_withCoupon_appliesDiscount() {
        UUID userId = UUID.randomUUID();
        when(adminClient.validatePublic("EARLY50")).thenReturn(
                PromoCodeValidation.builder().valid(true).discountPercent(50).code("EARLY50").build());

        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("redirect_url", Map.of("regular", "u", "express", "m"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        payTechService.createPayment(userId, "PREMIUM", "EARLY50");

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("1495"));
        assertThat(captor.getValue().getDiscountPercent()).isEqualTo(50);
        assertThat(captor.getValue().getOriginalAmount()).isEqualByComparingTo(new BigDecimal("2990"));
    }

    @Test
    @DisplayName("createPayment FREE → InvalidPlanException")
    void createPayment_free_throws() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> payTechService.createPayment(userId, "FREE", null))
                .isInstanceOf(InvalidPlanException.class);
    }

    @Test
    @DisplayName("createPayment success!=1 → PaymentException")
    @SuppressWarnings("unchecked")
    void createPayment_apiFailure_throws() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("success", 0);
        body.put("message", "Clés API invalides");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertThatThrownBy(() -> payTechService.createPayment(userId, "PREMIUM", null))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Clés API invalides");
    }

    @Test
    @DisplayName("createPayment erreur réseau → PaymentException")
    @SuppressWarnings("unchecked")
    void createPayment_networkError_throws() {
        UUID userId = UUID.randomUUID();
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> payTechService.createPayment(userId, "PREMIUM", null))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Impossible de contacter PayTech");
    }

    @Test
    @DisplayName("createPayment redirect_url string → fallback regular=express")
    @SuppressWarnings("unchecked")
    void createPayment_redirectUrlAsString_fallback() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("redirect_url", "https://paytech.sn/single-url");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        PayTechPaymentResponse result = payTechService.createPayment(userId, "PREMIUM", null);

        assertThat(result.getRedirectUrl()).isEqualTo("https://paytech.sn/single-url");
        assertThat(result.getMobileRedirectUrl()).isEqualTo("https://paytech.sn/single-url");
    }

    @Test
    @DisplayName("createPayment coupon invalide → pas de réduction appliquée")
    @SuppressWarnings("unchecked")
    void createPayment_invalidCoupon_noDiscount() {
        UUID userId = UUID.randomUUID();
        when(adminClient.validatePublic("EXPIRED")).thenReturn(
                PromoCodeValidation.builder().valid(false).reason("Expiré").build());

        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("redirect_url", Map.of("regular", "u", "express", "m"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        payTechService.createPayment(userId, "PREMIUM", "EXPIRED");

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("2990"));
        assertThat(captor.getValue().getDiscountPercent()).isNull();
    }

    @Test
    @DisplayName("baseUrl est toujours paytech.sn/api (env juste dans le payload)")
    void baseUrl_isAlwaysPaytechSn() {
        assertThat(config.getBaseUrl()).isEqualTo("https://paytech.sn/api");
    }
}
