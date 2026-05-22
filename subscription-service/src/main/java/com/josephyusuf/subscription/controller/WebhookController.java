package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.service.PayDunyaWebhookService;
import com.josephyusuf.subscription.service.PayTechWebhookService;
import com.josephyusuf.subscription.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final String INVALID_BODY = "invalid";

    private final WebhookService webhookService;
    private final PayDunyaWebhookService payDunyaWebhookService;
    private final PayTechWebhookService payTechWebhookService;

    @PostMapping("/stripe")
    public ResponseEntity<String> stripe(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String signature) {
        try {
            webhookService.processStripeWebhook(payload, signature);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Erreur traitement webhook Stripe : {}", e.getMessage());
            return ResponseEntity.badRequest().body(INVALID_BODY);
        }
    }

    @PostMapping("/paydunya")
    public ResponseEntity<String> paydunya(@RequestBody Map<String, Object> payload) {
        try {
            payDunyaWebhookService.handleCallback(payload);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Erreur traitement webhook PayDunya : {}", e.getMessage());
            return ResponseEntity.badRequest().body(INVALID_BODY);
        }
    }

    @PostMapping("/paytech")
    public ResponseEntity<String> paytech(@RequestBody Map<String, Object> payload) {
        try {
            payTechWebhookService.handleIPN(payload);
            return ResponseEntity.ok("ok");
        } catch (SecurityException e) {
            log.warn("PayTech IPN signature invalide : {}", e.getMessage());
            return ResponseEntity.status(401).body("invalid signature");
        } catch (Exception e) {
            log.error("Erreur traitement webhook PayTech : {}", e.getMessage());
            return ResponseEntity.badRequest().body(INVALID_BODY);
        }
    }
}
