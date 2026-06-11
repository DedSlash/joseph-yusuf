package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.service.PaddleWebhookService;
import com.josephyusuf.subscription.service.PayDunyaWebhookService;
import com.josephyusuf.subscription.service.PayTechWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final String INVALID_BODY = "invalid";

    private final PayDunyaWebhookService payDunyaWebhookService;
    private final PayTechWebhookService payTechWebhookService;
    private final PaddleWebhookService paddleWebhookService;

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

    @PostMapping(value = "/paytech", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> paytechForm(@RequestParam Map<String, String> params) {
        log.info("PayTech IPN reçu (form-urlencoded) : {}", params.keySet());
        Map<String, Object> payload = Map.copyOf(params);
        return handlePaytechIPN(payload);
    }

    @PostMapping(value = "/paytech", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> paytechJson(@RequestBody Map<String, Object> payload) {
        log.info("PayTech IPN reçu (json) : {}", payload.keySet());
        return handlePaytechIPN(payload);
    }

    private ResponseEntity<String> handlePaytechIPN(Map<String, Object> payload) {
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

    /**
     * Paddle webhook IPN.
     * IMPORTANT : on reçoit le body BRUT (String) car la vérification HMAC-SHA256
     * du header Paddle-Signature exige les octets exacts envoyés par Paddle.
     * Cf. https://developer.paddle.com/webhooks/signature-verification
     */
    @PostMapping(value = "/paddle", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> paddle(@RequestBody String payload,
                                         @RequestHeader("Paddle-Signature") String signature) {
        try {
            paddleWebhookService.handleWebhook(payload, signature);
            return ResponseEntity.ok("ok");
        } catch (SecurityException e) {
            log.warn("Paddle webhook signature invalide : {}", e.getMessage());
            return ResponseEntity.status(401).body("invalid signature");
        } catch (Exception e) {
            log.error("Erreur traitement webhook Paddle : {}", e.getMessage());
            return ResponseEntity.badRequest().body(INVALID_BODY);
        }
    }
}
