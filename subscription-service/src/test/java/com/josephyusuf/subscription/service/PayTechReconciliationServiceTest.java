package com.josephyusuf.subscription.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayTechReconciliationServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private SubscriptionService subscriptionService;
    @Mock private AuthClient authClient;
    @Mock private AdminClient adminClient;

    private PayTechConfig config;
    private PayTechReconciliationService service;

    @BeforeEach
    void setUp() {
        config = new PayTechConfig();
        config.setApiKey("key");
        config.setApiSecret("secret");
        service = new PayTechReconciliationService(
                config, restTemplate, subscriptionService, authClient, adminClient, new ObjectMapper());
    }

    private Transaction pendingTx(String token, String promoCode) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .provider(PaymentProvider.WAVE)
                .transactionId("JY-aaaaaaaa-1700000000000")
                .providerToken(token)
                .amount(BigDecimal.valueOf(2990))
                .currency("XOF")
                .plan(PlanTier.PREMIUM)
                .status(TransactionStatus.PENDING)
                .promoCode(promoCode)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubGetStatus(Map<String, Object> body) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET),
                any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    @Test
    @DisplayName("reconcile - PayTech répond sale_complete → activateAfterPayment + sync auth")
    void reconcile_saleComplete_activates() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("type_event", "sale_complete");
        body.put("custom_field", "{\"userId\":\"" + tx.getUserId() + "\",\"planTier\":\"PREMIUM\"}");
        stubGetStatus(body);

        service.reconcile(tx);

        verify(subscriptionService).activateAfterPayment(
                tx.getUserId(), PlanTier.PREMIUM, PaymentProvider.PAYTECH, tx.getTransactionId());
        verify(authClient).updatePlan(any(PlanUpdateRequest.class));
    }

    @Test
    @DisplayName("reconcile - sale_complete avec coupon dans la tx → AdminClient.apply")
    void reconcile_saleComplete_withTxCoupon_appliesPromo() {
        Transaction tx = pendingTx("pt_tok_xyz", "EARLY50");
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("type_event", "sale_complete");
        stubGetStatus(body);

        service.reconcile(tx);

        ArgumentCaptor<PromoCodeApplyRequest> captor = ArgumentCaptor.forClass(PromoCodeApplyRequest.class);
        verify(adminClient).apply(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("EARLY50");
        assertThat(captor.getValue().getUserId()).isEqualTo(tx.getUserId());
    }

    @Test
    @DisplayName("reconcile - sale_canceled → markTransactionFailed")
    void reconcile_saleCanceled_marksFailed() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("type_event", "sale_canceled");
        stubGetStatus(body);

        service.reconcile(tx);

        verify(subscriptionService).markTransactionFailed(eq(tx.getTransactionId()), anyString());
        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
    }

    @Test
    @DisplayName("reconcile - sale_pending → aucune action")
    void reconcile_salePending_noop() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("type_event", "sale_pending");
        stubGetStatus(body);

        service.reconcile(tx);

        verify(subscriptionService, never()).activateAfterPayment(any(), any(), any(), any());
        verify(subscriptionService, never()).markTransactionFailed(any(), any());
    }

    @Test
    @DisplayName("reconcile - statut non PENDING → PaymentException")
    void reconcile_notPending_rejected() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        tx.setStatus(TransactionStatus.SUCCEEDED);

        assertThatThrownBy(() -> service.reconcile(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("PENDING");

        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class),
                any(HttpEntity.class), any(Class.class));
    }

    @Test
    @DisplayName("reconcile - provider PAYDUNYA → PaymentException")
    void reconcile_paydunya_rejected() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        tx.setProvider(PaymentProvider.PAYDUNYA);

        assertThatThrownBy(() -> service.reconcile(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("PAYDUNYA");
    }

    @Test
    @DisplayName("reconcile - token absent → PaymentException (transaction antérieure à V10)")
    void reconcile_missingToken_rejected() {
        Transaction tx = pendingTx(null, null);

        assertThatThrownBy(() -> service.reconcile(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Token PayTech absent");
    }

    @Test
    @DisplayName("reconcile - PayTech success=0 → PaymentException avec message PayTech")
    void reconcile_apiFailure_throws() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        Map<String, Object> body = new HashMap<>();
        body.put("success", 0);
        body.put("message", "Token invalide");
        stubGetStatus(body);

        assertThatThrownBy(() -> service.reconcile(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Token invalide");
    }

    @Test
    @DisplayName("reconcile - erreur réseau → PaymentException")
    @SuppressWarnings("unchecked")
    void reconcile_networkError_throws() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET),
                any(HttpEntity.class), eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.reconcile(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Impossible de contacter PayTech");
    }

    @Test
    @DisplayName("reconcile - URL appelée contient token_payment et API headers")
    @SuppressWarnings("unchecked")
    void reconcile_passesTokenInUrlAndHeaders() {
        Transaction tx = pendingTx("pt_tok_xyz", null);
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("type_event", "sale_pending");
        stubGetStatus(body);

        service.reconcile(tx);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity<Void>> reqCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET),
                reqCaptor.capture(), any(Class.class));
        assertThat(urlCaptor.getValue()).isEqualTo(
                "https://paytech.sn/api/payment/get-status?token_payment=pt_tok_xyz");
        assertThat(reqCaptor.getValue().getHeaders().getFirst("API_KEY")).isEqualTo("key");
        assertThat(reqCaptor.getValue().getHeaders().getFirst("API_SECRET")).isEqualTo("secret");
    }
}
