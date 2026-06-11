package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.service.PayDunyaWebhookService;
import com.josephyusuf.subscription.service.PayTechWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookController")
class WebhookControllerTest {

    @Mock private PayDunyaWebhookService payDunyaWebhookService;
    @Mock private PayTechWebhookService payTechWebhookService;

    @InjectMocks
    private WebhookController controller;

    @Test
    @DisplayName("paydunya success → 200 ok")
    void paydunyaSuccess() {
        Map<String, Object> payload = Map.of("status", "completed");
        doNothing().when(payDunyaWebhookService).handleCallback(payload);

        ResponseEntity<String> response = controller.paydunya(payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ok");
    }

    @Test
    @DisplayName("paydunya error → 400 invalid")
    void paydunyaError() {
        Map<String, Object> payload = Map.of("status", "bad");
        doThrow(new RuntimeException("oops")).when(payDunyaWebhookService).handleCallback(payload);

        ResponseEntity<String> response = controller.paydunya(payload);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("invalid");
    }

    @Test
    @DisplayName("paytech JSON success → 200 ok")
    void paytechJsonSuccess() {
        Map<String, Object> payload = Map.of("type_event", "sale_complete");
        doNothing().when(payTechWebhookService).handleIPN(payload);

        ResponseEntity<String> response = controller.paytechJson(payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("paytech form-urlencoded success → 200 ok")
    void paytechFormSuccess() {
        Map<String, String> params = Map.of("type_event", "sale_complete");
        doNothing().when(payTechWebhookService).handleIPN(any());

        ResponseEntity<String> response = controller.paytechForm(params);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("paytech signature invalide → 401")
    void paytechInvalidSignature() {
        Map<String, Object> payload = Map.of("type_event", "sale_complete");
        doThrow(new SecurityException("bad sig")).when(payTechWebhookService).handleIPN(payload);

        ResponseEntity<String> response = controller.paytechJson(payload);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
