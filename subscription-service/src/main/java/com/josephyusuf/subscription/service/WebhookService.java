package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.entity.ProcessedWebhookEvent;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.repository.ProcessedWebhookEventRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    static final String EVENT_PI_SUCCEEDED = "payment_intent.succeeded";
    static final String EVENT_PI_FAILED = "payment_intent.payment_failed";
    static final String EVENT_CHARGE_REFUNDED = "charge.refunded";

    private final StripeConfig stripeConfig;
    private final ProcessedWebhookEventRepository processedEventRepository;
    private final SubscriptionService subscriptionService;
    private final AdminClient adminClient;

    @Transactional
    public void processStripeWebhook(String payload, String sigHeader) {
        Event event = verifyAndParse(payload, sigHeader);

        if (processedEventRepository.existsById(event.getId())) {
            log.info("Webhook Stripe déjà traité (idempotence) eventId={}", event.getId());
            return;
        }

        log.info("Webhook Stripe reçu eventId={} type={}", event.getId(), event.getType());

        switch (event.getType()) {
            case EVENT_PI_SUCCEEDED -> handleSucceeded(event);
            case EVENT_PI_FAILED -> handleFailed(event);
            case EVENT_CHARGE_REFUNDED -> handleRefunded(event);
            default -> log.info("Événement Stripe non géré : {}", event.getType());
        }

        processedEventRepository.save(ProcessedWebhookEvent.builder()
                .eventId(event.getId())
                .provider(PaymentProvider.STRIPE)
                .eventType(event.getType())
                .build());
    }

    private Event verifyAndParse(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, stripeConfig.getStripeWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Signature Stripe invalide : {}", e.getMessage());
            throw new PaymentException("Signature webhook Stripe invalide", e);
        }
    }

    private void handleSucceeded(Event event) {
        PaymentIntent intent = extract(event, PaymentIntent.class);
        if (intent == null) return;

        UUID userId = parseUserId(intent.getMetadata().get("userId"));
        PlanTier plan = parsePlan(intent.getMetadata().get("plan"));
        if (userId == null || plan == null) {
            log.error("Metadata Stripe incomplètes pi={}", intent.getId());
            return;
        }

        subscriptionService.activateAfterPayment(userId, plan, PaymentProvider.STRIPE, intent.getId());

        // Enregistrer l'usage du code promo après confirmation du paiement
        String promoCode = intent.getMetadata().get("promoCode");
        if (promoCode != null && !promoCode.isBlank()) {
            try {
                adminClient.apply(PromoCodeApplyRequest.builder()
                        .code(promoCode)
                        .userId(userId)
                        .transactionId(intent.getId())
                        .build());
                log.info("Usage code promo {} enregistré pour userId={} tx={}", promoCode, userId, intent.getId());
            } catch (Exception e) {
                log.error("Échec enregistrement usage code promo {} userId={} tx={} : {}",
                        promoCode, userId, intent.getId(), e.getMessage());
            }
        }
    }

    private void handleFailed(Event event) {
        PaymentIntent intent = extract(event, PaymentIntent.class);
        if (intent == null) return;
        String reason = intent.getLastPaymentError() != null
                ? intent.getLastPaymentError().getMessage()
                : "Paiement échoué";
        subscriptionService.markTransactionFailed(intent.getId(), reason);
    }

    private void handleRefunded(Event event) {
        Charge charge = extract(event, Charge.class);
        if (charge == null || charge.getPaymentIntent() == null) {
            log.warn("Refund sans PaymentIntent associé");
            return;
        }
        subscriptionService.markTransactionRefunded(charge.getPaymentIntent());
    }

    private <T> T extract(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        // Tentative avec la version stricte d'abord
        if (deserializer.getObject().isPresent()) {
            Object obj = deserializer.getObject().get();
            return type.isInstance(obj) ? type.cast(obj) : null;
        }

        // Fallback : désérialisation sans vérification de version API
        // Nécessaire quand la version de l'événement Stripe diffère de celle de la SDK
        try {
            com.stripe.model.StripeObject raw = deserializer.deserializeUnsafe();
            return type.isInstance(raw) ? type.cast(raw) : null;
        } catch (Exception e) {
            log.error("Désérialisation impossible eventId={} type={} : {}", event.getId(), event.getType(), e.getMessage());
            return null;
        }
    }

    private UUID parseUserId(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private PlanTier parsePlan(String value) {
        try {
            return value == null ? null : PlanTier.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
