package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.config.PayTechConfig;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Déclenche le remboursement effectif côté PayTech via
 * {@code POST /payment/refund-payment}. À utiliser depuis l'endpoint admin
 * avant le marquage REFUNDED local — si PayTech refuse, la transaction reste
 * SUCCEEDED chez nous pour éviter le drift avec leur dashboard.
 *
 * Doc : https://doc.intech.sn/doc_paytech.php — §REFUND PAYMENT
 * Format body attendu : {@code application/x-www-form-urlencoded}, champ
 * unique {@code ref_command}. Headers {@code API_KEY} + {@code API_SECRET}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayTechRefundService {

    private final PayTechConfig config;
    private final RestTemplate restTemplate;

    /**
     * Déclenche le refund côté PayTech. La transaction doit appartenir à un
     * provider relayé par PayTech (PAYTECH, WAVE, ORANGE_MONEY, FREE_MONEY,
     * CARTE) sinon on lève {@link PaymentException} — PayDunya a son propre
     * flux.
     */
    public void refund(Transaction transaction) {
        if (!isPayTechProvider(transaction.getProvider())) {
            throw new PaymentException(
                    "Provider " + transaction.getProvider() + " ne supporte pas le refund automatique PayTech");
        }
        if (transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
            throw new PaymentException("ref_command manquant sur la transaction");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("API_KEY", config.getApiKey());
        headers.set("API_SECRET", config.getApiSecret());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("ref_command", transaction.getTransactionId());

        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/payment/refund-payment",
                    new HttpEntity<>(body, headers),
                    (Class<Map<String, Object>>) (Class<?>) Map.class);
        } catch (Exception e) {
            log.error("PayTech /refund-payment erreur réseau ref={} : {}",
                    transaction.getTransactionId(), e.getMessage());
            throw new PaymentException(
                    "Impossible de contacter PayTech pour le remboursement. Réessayez.");
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new PaymentException("Réponse PayTech /refund-payment vide");
        }

        if (!"1".equals(String.valueOf(responseBody.get("success")))) {
            String message = String.valueOf(responseBody.getOrDefault("message", "erreur inconnue"));
            log.error("PayTech refund refusé ref={} : {}",
                    transaction.getTransactionId(), message);
            throw new PaymentException("PayTech a refusé le remboursement : " + message);
        }

        log.info("PayTech refund accepté ref={} provider={}",
                transaction.getTransactionId(), transaction.getProvider());
    }

    private boolean isPayTechProvider(PaymentProvider provider) {
        return provider == PaymentProvider.PAYTECH
                || provider == PaymentProvider.WAVE
                || provider == PaymentProvider.ORANGE_MONEY
                || provider == PaymentProvider.FREE_MONEY
                || provider == PaymentProvider.CARTE;
    }
}
