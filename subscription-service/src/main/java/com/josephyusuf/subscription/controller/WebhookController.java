package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/stripe")
    public ResponseEntity<String> stripe(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String signature) {
        try {
            webhookService.processStripeWebhook(payload, signature);
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Erreur traitement webhook Stripe : {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid");
        }
    }
}
