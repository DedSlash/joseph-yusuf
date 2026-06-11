package com.josephyusuf.subscription.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.dto.PayTechPaymentResponse;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import com.josephyusuf.subscription.enums.PaymentProvider;
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
        config.setRefundNotifUrl("https://api.josephyusuf.com/api/webhooks/paytech");

        payTechService = new PayTechService(config, restTemplate, subscriptionService,
                adminClient, new ObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private void stubPayTechOk() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("redirect_url", Map.of("regular", "https://paytech.sn/payment/checkout/abc",
                "express", "https://paytech.sn/payment/mobile/abc"));
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    @Test
    @DisplayName("createPayment PREMIUM sans method code → 2990 XOF + provider PAYTECH (agrégateur)")
    void createPayment_premium_noMethodCode() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        PayTechPaymentResponse result = payTechService.createPayment(userId, "PREMIUM", null, null);

        assertThat(result.getRefCommand()).startsWith("JY-");
        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("2990"));
        assertThat(captor.getValue().getProvider()).isEqualTo(PaymentProvider.PAYTECH);
    }

    @Test
    @DisplayName("createPayment PREMIUM_PLUS → 5990 XOF")
    void createPayment_premiumPlus() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM_PLUS", null, null);

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("5990"));
    }

    @Test
    @DisplayName("createPayment avec method code 'Wave' → provider WAVE + target_payment dans payload")
    void createPayment_withMethodCodeWave_setsTargetPayment() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", null, "Wave");

        ArgumentCaptor<HttpEntity<Map<String, Object>>> reqCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), reqCaptor.capture(), any(Class.class));
        Map<String, Object> sentBody = reqCaptor.getValue().getBody();
        assertThat(sentBody).containsEntry("target_payment", "Wave");

        ArgumentCaptor<PendingTransactionParams> txCaptor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.WAVE);
    }

    @Test
    @DisplayName("createPayment avec method code 'Orange Money' → provider ORANGE_MONEY")
    void createPayment_orangeMoney() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", null, "Orange Money");

        ArgumentCaptor<PendingTransactionParams> txCaptor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.ORANGE_MONEY);
    }

    @Test
    @DisplayName("createPayment avec method code 'Free Money' → provider FREE_MONEY")
    void createPayment_freeMoney() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", null, "Free Money");

        ArgumentCaptor<PendingTransactionParams> txCaptor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.FREE_MONEY);
    }

    @Test
    @DisplayName("createPayment avec method code 'Carte Bancaire' → provider CARTE")
    void createPayment_card() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", null, "Carte Bancaire");

        ArgumentCaptor<PendingTransactionParams> txCaptor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(txCaptor.capture());
        assertThat(txCaptor.getValue().getProvider()).isEqualTo(PaymentProvider.CARTE);
    }

    @Test
    @DisplayName("createPayment EARLY50 → 1495 XOF + discount tracé")
    void createPayment_withCoupon() {
        UUID userId = UUID.randomUUID();
        when(adminClient.validatePublic("EARLY50")).thenReturn(
                PromoCodeValidation.builder().valid(true).discountPercent(50).code("EARLY50").build());
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", "EARLY50", "Wave");

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
        assertThatThrownBy(() -> payTechService.createPayment(userId, "FREE", null, null))
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

        assertThatThrownBy(() -> payTechService.createPayment(userId, "PREMIUM", null, null))
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

        assertThatThrownBy(() -> payTechService.createPayment(userId, "PREMIUM", null, null))
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

        PayTechPaymentResponse result = payTechService.createPayment(userId, "PREMIUM", null, null);

        assertThat(result.getRedirectUrl()).isEqualTo("https://paytech.sn/single-url");
        assertThat(result.getMobileRedirectUrl()).isEqualTo("https://paytech.sn/single-url");
    }

    @Test
    @DisplayName("createPayment coupon invalide → pas de réduction")
    void createPayment_invalidCoupon_noDiscount() {
        UUID userId = UUID.randomUUID();
        when(adminClient.validatePublic("EXPIRED")).thenReturn(
                PromoCodeValidation.builder().valid(false).reason("Expiré").build());
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", "EXPIRED", null);

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("2990"));
        assertThat(captor.getValue().getDiscountPercent()).isNull();
    }

    @Test
    @DisplayName("baseUrl est toujours paytech.sn/api")
    void baseUrl_isAlwaysPaytechSn() {
        assertThat(config.getBaseUrl()).isEqualTo("https://paytech.sn/api");
    }

    @Test
    @DisplayName("createPayment capture le token PayTech depuis la réponse")
    @SuppressWarnings("unchecked")
    void createPayment_capturesProviderToken() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("token", "pt_token_abc123");
        body.put("redirect_url", "https://paytech.sn/payment/checkout/abc");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        payTechService.createPayment(userId, "PREMIUM", null, null);

        ArgumentCaptor<PendingTransactionParams> captor =
                ArgumentCaptor.forClass(PendingTransactionParams.class);
        verify(subscriptionService).recordPendingTransaction(captor.capture());
        assertThat(captor.getValue().getProviderToken()).isEqualTo("pt_token_abc123");
    }

    @Test
    @DisplayName("createPayment envoie refund_notif_url et ipn_url dans le payload")
    @SuppressWarnings("unchecked")
    void createPayment_includesRefundNotifUrl() {
        UUID userId = UUID.randomUUID();
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", null, null);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> reqCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), reqCaptor.capture(), any(Class.class));
        Map<String, Object> sentBody = reqCaptor.getValue().getBody();
        assertThat(sentBody)
                .containsEntry("ipn_url", "https://api.josephyusuf.com/api/webhooks/paytech")
                .containsEntry("refund_notif_url", "https://api.josephyusuf.com/api/webhooks/paytech");
    }

    @Test
    @DisplayName("createPayment omet refund_notif_url si non configuré")
    @SuppressWarnings("unchecked")
    void createPayment_skipsRefundNotifUrlIfBlank() {
        UUID userId = UUID.randomUUID();
        config.setRefundNotifUrl(null);
        stubPayTechOk();

        payTechService.createPayment(userId, "PREMIUM", null, null);

        ArgumentCaptor<HttpEntity<Map<String, Object>>> reqCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), reqCaptor.capture(), any(Class.class));
        Map<String, Object> sentBody = reqCaptor.getValue().getBody();
        assertThat(sentBody).doesNotContainKey("refund_notif_url");
    }
}
