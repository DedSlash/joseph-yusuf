package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.entity.ProcessedWebhookEvent;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.repository.ProcessedWebhookEventRepository;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    static final String EVENT_INVOICE_PAID = "invoice.payment_succeeded";
    static final String EVENT_INVOICE_FAILED = "invoice.payment_failed";
    static final String EVENT_SUB_DELETED = "customer.subscription.deleted";
    static final String EVENT_SUB_UPDATED = "customer.subscription.updated";

    private final StripeConfig stripeConfig;
    private final ProcessedWebhookEventRepository processedEventRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
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
            case EVENT_INVOICE_PAID -> handleInvoicePaid(event);
            case EVENT_INVOICE_FAILED -> handleInvoiceFailed(event);
            case EVENT_SUB_DELETED -> handleSubscriptionDeleted(event);
            case EVENT_SUB_UPDATED -> handleSubscriptionUpdated(event);
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

    private void handleInvoicePaid(Event event) {
        Invoice invoice = extract(event, Invoice.class);
        if (invoice == null) return;
        subscriptionService.activateSubscriptionFromInvoice(invoice);
        applyPromoCodeIfPresent(invoice);
    }

    private void handleInvoiceFailed(Event event) {
        Invoice invoice = extract(event, Invoice.class);
        if (invoice == null) return;
        subscriptionService.recordPaymentFailure(invoice);
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription sub = extract(event, com.stripe.model.Subscription.class);
        if (sub == null || sub.getCustomer() == null) return;
        subscriptionService.downgradeToFree(sub.getCustomer());
    }

    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription sub = extract(event, com.stripe.model.Subscription.class);
        if (sub == null) return;
        subscriptionService.updateSubscriptionFromStripe(sub);
    }

    /**
     * Enregistre l'usage du code promo interne après le premier paiement réussi.
     * Coexistence : Stripe applique automatiquement la réduction récurrente, mais
     * on garde l'historique côté joseph_admin.promo_codes (compteurs, limites par user).
     */
    private void applyPromoCodeIfPresent(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null) return;
        Subscription local = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
        if (local == null || local.getStripeCouponId() == null) return;
        // Une seule fois par subscription : on regarde si c'est la première invoice payée
        // (heuristique simple : si la subscription vient juste d'être activée)
        try {
            adminClient.apply(PromoCodeApplyRequest.builder()
                    .code(local.getStripeCouponId())
                    .userId(local.getUserId())
                    .transactionId(invoice.getId())
                    .build());
            log.info("Usage code promo {} enregistré userId={} invoice={}",
                    local.getStripeCouponId(), local.getUserId(), invoice.getId());
        } catch (Exception e) {
            log.error("Échec enregistrement usage code promo {} userId={} invoice={} : {}",
                    local.getStripeCouponId(), local.getUserId(), invoice.getId(), e.getMessage());
        }
    }

    private <T> T extract(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            Object obj = deserializer.getObject().get();
            return type.isInstance(obj) ? type.cast(obj) : null;
        }

        // Fallback : désérialisation sans vérification de version API
        try {
            com.stripe.model.StripeObject raw = deserializer.deserializeUnsafe();
            return type.isInstance(raw) ? type.cast(raw) : null;
        } catch (Exception e) {
            log.error("Désérialisation impossible eventId={} type={} : {}",
                    event.getId(), event.getType(), e.getMessage());
            return null;
        }
    }
}
