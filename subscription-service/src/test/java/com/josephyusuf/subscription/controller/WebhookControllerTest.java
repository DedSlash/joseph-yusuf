package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookController")
class WebhookControllerTest {

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController controller;

    @Test
    @DisplayName("stripe webhook success → 200 ok")
    void stripeSuccess() {
        doNothing().when(webhookService).processStripeWebhook("payload", "sig_123");

        ResponseEntity<String> response = controller.stripe("payload", "sig_123");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ok");
    }

    @Test
    @DisplayName("stripe webhook error → 400 invalid")
    void stripeError() {
        doThrow(new RuntimeException("bad sig")).when(webhookService).processStripeWebhook("bad", "sig");

        ResponseEntity<String> response = controller.stripe("bad", "sig");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("invalid");
    }
}
