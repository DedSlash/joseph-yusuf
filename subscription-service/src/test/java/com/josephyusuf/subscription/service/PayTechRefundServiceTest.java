package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.config.PayTechConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
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
class PayTechRefundServiceTest {

    @Mock private RestTemplate restTemplate;

    private PayTechConfig config;
    private PayTechRefundService service;

    @BeforeEach
    void setUp() {
        config = new PayTechConfig();
        config.setApiKey("test_key");
        config.setApiSecret("test_secret");
        service = new PayTechRefundService(config, restTemplate);
    }

    private Transaction sampleTx(PaymentProvider provider) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .provider(provider)
                .transactionId("JY-aaaaaaaa-1700000000000")
                .amount(BigDecimal.valueOf(2990))
                .currency("XOF")
                .plan(PlanTier.PREMIUM)
                .status(TransactionStatus.SUCCEEDED)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubPayTechOk() {
        Map<String, Object> body = new HashMap<>();
        body.put("success", 1);
        body.put("message", "Refund OK");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    @Test
    @DisplayName("refund WAVE → POST /payment/refund-payment avec ref_command form-urlencoded")
    @SuppressWarnings("unchecked")
    void refund_wave_postsFormEncoded() {
        Transaction tx = sampleTx(PaymentProvider.WAVE);
        stubPayTechOk();

        service.refund(tx);

        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> captor =
                ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://paytech.sn/api/payment/refund-payment"),
                captor.capture(), any(Class.class));
        HttpEntity<MultiValueMap<String, String>> sent = captor.getValue();
        assertThat(sent.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
        assertThat(sent.getHeaders().getFirst("API_KEY")).isEqualTo("test_key");
        assertThat(sent.getHeaders().getFirst("API_SECRET")).isEqualTo("test_secret");
        assertThat(sent.getBody().getFirst("ref_command")).isEqualTo("JY-aaaaaaaa-1700000000000");
    }

    @Test
    @DisplayName("refund accepte tous les providers PayTech : PAYTECH, WAVE, ORANGE_MONEY, FREE_MONEY, CARTE")
    void refund_acceptsAllPayTechProviders() {
        stubPayTechOk();
        for (PaymentProvider p : new PaymentProvider[]{
                PaymentProvider.PAYTECH, PaymentProvider.WAVE,
                PaymentProvider.ORANGE_MONEY, PaymentProvider.FREE_MONEY,
                PaymentProvider.CARTE}) {
            service.refund(sampleTx(p));
        }
    }

    @Test
    @DisplayName("refund PAYDUNYA → PaymentException (provider non supporté)")
    void refund_paydunya_rejected() {
        Transaction tx = sampleTx(PaymentProvider.PAYDUNYA);

        assertThatThrownBy(() -> service.refund(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("PAYDUNYA");

        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    @DisplayName("refund sans ref_command → PaymentException, pas d'appel réseau")
    void refund_missingRefCommand_rejected() {
        Transaction tx = sampleTx(PaymentProvider.WAVE);
        tx.setTransactionId(null);

        assertThatThrownBy(() -> service.refund(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("ref_command");

        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any(Class.class));
    }

    @Test
    @DisplayName("refund réponse success=0 → PaymentException avec message PayTech")
    @SuppressWarnings("unchecked")
    void refund_apiSuccessZero_throws() {
        Transaction tx = sampleTx(PaymentProvider.WAVE);
        Map<String, Object> body = new HashMap<>();
        body.put("success", 0);
        body.put("message", "Transaction déjà remboursée");
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertThatThrownBy(() -> service.refund(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Transaction déjà remboursée");
    }

    @Test
    @DisplayName("refund erreur réseau → PaymentException 'Impossible de contacter PayTech'")
    @SuppressWarnings("unchecked")
    void refund_networkError_throws() {
        Transaction tx = sampleTx(PaymentProvider.WAVE);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.refund(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Impossible de contacter PayTech");
    }

    @Test
    @DisplayName("refund body null → PaymentException")
    @SuppressWarnings("unchecked")
    void refund_nullBody_throws() {
        Transaction tx = sampleTx(PaymentProvider.WAVE);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class),
                eq((Class<Map<String, Object>>) (Class<?>) Map.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThatThrownBy(() -> service.refund(tx))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("vide");
    }
}
