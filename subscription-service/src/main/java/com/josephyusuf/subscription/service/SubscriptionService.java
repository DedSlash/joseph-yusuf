package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.config.StripeConfig;
import com.josephyusuf.subscription.dto.CreateSubscriptionRequest;
import com.josephyusuf.subscription.dto.CreateSubscriptionResponse;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.PlanUpdateRequest;
import com.josephyusuf.subscription.dto.StripeSubscriptionResult;
import com.josephyusuf.subscription.dto.SubscriptionResponse;
import com.josephyusuf.subscription.dto.TransactionResponse;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.mapper.SubscriptionMapper;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.josephyusuf.subscription.repository.TransactionRepository;
import com.stripe.model.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionMapper mapper;
    private final AuthClient authClient;
    private final StripeService stripeService;
    private final StripeConfig stripeConfig;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               TransactionRepository transactionRepository,
                               SubscriptionMapper mapper,
                               AuthClient authClient,
                               StripeService stripeService,
                               StripeConfig stripeConfig) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.authClient = authClient;
        this.stripeService = stripeService;
        this.stripeConfig = stripeConfig;
    }

    /**
     * Orchestre la création d'une subscription Stripe + persistance locale.
     * Le statut initial est {@code PENDING} : il sera marqué {@code ACTIVE}
     * par {@code invoice.payment_succeeded} après confirmation du paiement.
     */
    @Transactional
    public CreateSubscriptionResponse createStripeSubscription(UUID userId,
                                                               String email,
                                                               CreateSubscriptionRequest request) {
        if (request.getPlanTier() == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement");
        }

        StripeSubscriptionResult result = stripeService.createSubscription(
                userId, email, request.getPlanTier(), request.getCurrency(),
                request.getCouponCode(), request.getPaymentMethodId());

        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> Subscription.builder().userId(userId).build());

        subscription.setPlan(request.getPlanTier());
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setProvider(PaymentProvider.STRIPE);
        subscription.setStripeCustomerId(result.getStripeCustomerId());
        subscription.setStripeSubscriptionId(result.getStripeSubscriptionId());
        subscription.setStripePriceId(result.getStripePriceId());
        subscription.setStripeCouponId(result.getAppliedCouponId());
        subscription.setCouponDuration(result.getCouponDuration());
        subscription.setCurrency(request.getCurrency().toUpperCase());
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancelledAt(null);
        subscriptionRepository.save(subscription);

        log.info("Subscription Stripe persistée userId={} sub={} status={}",
                userId, result.getStripeSubscriptionId(), result.getStatus());

        return CreateSubscriptionResponse.builder()
                .subscriptionId(result.getStripeSubscriptionId())
                .clientSecret(result.getClientSecret())
                .status(result.getStatus())
                .build();
    }

    /**
     * Activation manuelle d'une subscription (utilisé par AdminTransactionService.forceActivate).
     * Court-circuit hors flow Stripe Subscriptions — utile pour Wave/Orange Money ou
     * pour rattraper une transaction Stripe dont le webhook a été perdu.
     */
    @Transactional
    public Subscription activateAfterPayment(UUID userId, PlanTier plan, PaymentProvider provider,
                                             String externalTxId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .map(existing -> {
                    existing.setPlan(plan);
                    existing.setStatus(SubscriptionStatus.ACTIVE);
                    existing.setProvider(provider);
                    existing.setStartedAt(Instant.now());
                    existing.setExpiresAt(Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS));
                    existing.setCancelledAt(null);
                    return existing;
                })
                .orElseGet(() -> Subscription.builder()
                        .userId(userId)
                        .plan(plan)
                        .status(SubscriptionStatus.ACTIVE)
                        .provider(provider)
                        .expiresAt(Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS))
                        .build());

        Subscription saved = subscriptionRepository.save(subscription);

        if (externalTxId != null) {
            transactionRepository.findByTransactionId(externalTxId).ifPresent(tx -> {
                tx.setStatus(TransactionStatus.SUCCEEDED);
                tx.setSubscriptionId(saved.getId());
                transactionRepository.save(tx);
            });
        }

        syncPlanWithAuthService(userId, plan);
        log.info("Subscription activée manuellement userId={} plan={} provider={}", userId, plan, provider);
        return saved;
    }

    @Transactional
    public void markTransactionFailed(String externalTxId, String reason) {
        transactionRepository.findByTransactionId(externalTxId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setFailureReason(reason);
            transactionRepository.save(tx);
            log.info("Transaction marquée FAILED tx={} reason={}", externalTxId, reason);
        });
    }

    @Transactional
    public void markTransactionRefunded(String externalTxId) {
        transactionRepository.findByTransactionId(externalTxId).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.REFUNDED);
            transactionRepository.save(tx);
            log.info("Transaction marquée REFUNDED tx={}", externalTxId);
        });
    }

    @Transactional
    public Transaction recordPendingTransaction(PendingTransactionParams params) {
        if (params.getPlan() == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de transaction");
        }
        Transaction tx = Transaction.builder()
                .userId(params.getUserId())
                .plan(params.getPlan())
                .provider(params.getProvider())
                .transactionId(params.getExternalTxId())
                .amount(params.getAmount())
                .currency(params.getCurrency())
                .status(TransactionStatus.PENDING)
                .promoCode(params.getPromoCode())
                .discountPercent(params.getDiscountPercent())
                .originalAmount(params.getOriginalAmount())
                .build();
        return transactionRepository.save(tx);
    }

    /**
     * Confirme l'état d'une subscription Stripe (utilisé après confirmation
     * client-side du payment intent). Idempotent : le webhook
     * {@code invoice.payment_succeeded} fait le même travail en arrière-plan.
     */
    @Transactional
    public SubscriptionResponse confirmStripeSubscription(UUID userId, String stripeSubscriptionId) {
        com.stripe.model.Subscription stripeSub = stripeService.retrieveSubscription(stripeSubscriptionId);
        Subscription local = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription locale introuvable pour stripeSubscriptionId=" + stripeSubscriptionId));
        if (!userId.equals(local.getUserId())) {
            throw new InvalidPlanException("Subscription non autorisée pour cet utilisateur");
        }
        updateLocalFromStripe(local, stripeSub);
        Subscription saved = subscriptionRepository.save(local);
        syncPlanWithAuthService(userId, planFromStripeStatus(saved));
        return mapper.toResponse(saved);
    }

    /**
     * Webhook handler : {@code invoice.payment_succeeded}.
     * Marque la subscription ACTIVE et enregistre la transaction.
     */
    @Transactional
    public void activateSubscriptionFromInvoice(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null) {
            log.warn("Invoice sans subscription, ignoré invoice={}", invoice.getId());
            return;
        }
        Subscription local = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
        if (local == null) {
            log.warn("Subscription locale introuvable pour invoice={} sub={}", invoice.getId(), subscriptionId);
            return;
        }

        com.stripe.model.Subscription stripeSub = stripeService.retrieveSubscription(subscriptionId);
        updateLocalFromStripe(local, stripeSub);
        local.setStatus(SubscriptionStatus.ACTIVE);
        Subscription saved = subscriptionRepository.save(local);

        recordPaidTransaction(invoice, saved);
        syncPlanWithAuthService(local.getUserId(), local.getPlan());
        log.info("Subscription ACTIVE après invoice userId={} sub={} invoice={}",
                local.getUserId(), subscriptionId, invoice.getId());
    }

    /**
     * Webhook handler : {@code invoice.payment_failed}.
     * Enregistre l'échec ; Stripe Smart Retries va retenter automatiquement.
     */
    @Transactional
    public void recordPaymentFailure(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null) {
            return;
        }
        Subscription local = subscriptionRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
        if (local == null) {
            log.warn("Subscription locale introuvable pour failed invoice={} sub={}",
                    invoice.getId(), subscriptionId);
            return;
        }
        Transaction tx = Transaction.builder()
                .userId(local.getUserId())
                .subscriptionId(local.getId())
                .plan(local.getPlan())
                .provider(PaymentProvider.STRIPE)
                .transactionId(invoice.getId())
                .stripeInvoiceId(invoice.getId())
                .stripePaymentIntentId(invoice.getPaymentIntent())
                .amount(BigDecimal.valueOf(invoice.getAmountDue() == null ? 0L : invoice.getAmountDue()))
                .currency(invoice.getCurrency() == null ? "EUR" : invoice.getCurrency().toUpperCase())
                .status(TransactionStatus.FAILED)
                .failureReason("Échec paiement invoice Stripe")
                .build();
        transactionRepository.save(tx);
        log.info("Échec paiement enregistré userId={} sub={} invoice={}",
                local.getUserId(), subscriptionId, invoice.getId());
    }

    /**
     * Webhook handler : {@code customer.subscription.deleted}.
     * Tous les retries ont échoué ou annulation finale → downgrade FREE.
     */
    @Transactional
    public void downgradeToFree(String stripeCustomerId) {
        Subscription local = subscriptionRepository.findByStripeCustomerId(stripeCustomerId).orElse(null);
        if (local == null) {
            log.warn("Subscription locale introuvable pour customer={} (downgrade)", stripeCustomerId);
            return;
        }
        local.setStatus(SubscriptionStatus.CANCELLED);
        local.setCancelledAt(Instant.now());
        local.setPlan(PlanTier.FREE);
        local.setCancelAtPeriodEnd(false);
        subscriptionRepository.save(local);
        syncPlanWithAuthService(local.getUserId(), PlanTier.FREE);
        log.info("Downgrade vers FREE userId={} customer={}", local.getUserId(), stripeCustomerId);
    }

    /**
     * Webhook handler : {@code customer.subscription.updated}.
     * Met à jour currentPeriodEnd, status, cancelAtPeriodEnd, plan (upgrade/downgrade).
     */
    @Transactional
    public void updateSubscriptionFromStripe(com.stripe.model.Subscription stripeSub) {
        Subscription local = subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).orElse(null);
        if (local == null) {
            log.warn("Subscription locale introuvable pour update sub={}", stripeSub.getId());
            return;
        }
        updateLocalFromStripe(local, stripeSub);
        subscriptionRepository.save(local);
        log.info("Subscription mise à jour userId={} sub={} status={}",
                local.getUserId(), stripeSub.getId(), stripeSub.getStatus());
    }

    public SubscriptionResponse getCurrent(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Aucun abonnement trouvé pour userId=" + userId));
        SubscriptionResponse response = mapper.toResponse(subscription);
        enrichWithNextInvoiceAmount(subscription, response);
        return response;
    }

    /**
     * Annulation par le client (toggle auto-renew off ou cancel explicite).
     */
    @Transactional
    public SubscriptionResponse cancelStripeSubscription(UUID userId, boolean immediately) {
        Subscription local = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Aucun abonnement actif"));
        if (local.getStripeSubscriptionId() == null) {
            throw new InvalidPlanException("Aucune subscription Stripe à annuler");
        }
        com.stripe.model.Subscription stripeSub = stripeService.cancelSubscription(
                local.getStripeSubscriptionId(), immediately);
        updateLocalFromStripe(local, stripeSub);
        if (immediately) {
            local.setStatus(SubscriptionStatus.CANCELLED);
            local.setCancelledAt(Instant.now());
        }
        local.setAutoRenew(!stripeSub.getCancelAtPeriodEnd());
        Subscription saved = subscriptionRepository.save(local);
        log.info("Subscription Stripe annulée userId={} immediately={}", userId, immediately);
        return mapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse setAutoRenew(UUID userId, boolean autoRenew) {
        Subscription local = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Aucun abonnement actif"));
        if (local.getStripeSubscriptionId() != null) {
            com.stripe.model.Subscription stripeSub = autoRenew
                    ? reactivateStripeSubscription(local.getStripeSubscriptionId())
                    : stripeService.cancelSubscription(local.getStripeSubscriptionId(), false);
            updateLocalFromStripe(local, stripeSub);
        }
        local.setAutoRenew(autoRenew);
        Subscription saved = subscriptionRepository.save(local);
        log.info("autoRenew={} userId={}", autoRenew, userId);
        return mapper.toResponse(saved);
    }

    public Page<TransactionResponse> getHistory(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toResponse);
    }

    private com.stripe.model.Subscription reactivateStripeSubscription(String stripeSubscriptionId) {
        try {
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            return stripeSub.update(com.stripe.param.SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .build());
        } catch (com.stripe.exception.StripeException e) {
            log.error("Échec réactivation Stripe sub={} : {}", stripeSubscriptionId, e.getMessage());
            throw new com.josephyusuf.subscription.exception.PaymentException(
                    "Impossible de réactiver l'abonnement.", e);
        }
    }

    private void updateLocalFromStripe(Subscription local, com.stripe.model.Subscription stripeSub) {
        if (stripeSub.getCurrentPeriodStart() != null) {
            local.setCurrentPeriodStart(Instant.ofEpochSecond(stripeSub.getCurrentPeriodStart()));
        }
        if (stripeSub.getCurrentPeriodEnd() != null) {
            local.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));
            local.setExpiresAt(Instant.ofEpochSecond(stripeSub.getCurrentPeriodEnd()));
        }
        Boolean cancelAtEnd = stripeSub.getCancelAtPeriodEnd();
        if (cancelAtEnd != null) {
            local.setCancelAtPeriodEnd(cancelAtEnd);
            local.setAutoRenew(!cancelAtEnd);
        }
        // Status mapping
        SubscriptionStatus mapped = mapStripeStatus(stripeSub.getStatus());
        if (mapped != null) {
            local.setStatus(mapped);
        }
    }

    private void recordPaidTransaction(Invoice invoice, Subscription local) {
        if (invoice.getId() != null && transactionRepository.findByStripeInvoiceId(invoice.getId()).isPresent()) {
            return;
        }
        Transaction tx = Transaction.builder()
                .userId(local.getUserId())
                .subscriptionId(local.getId())
                .plan(local.getPlan())
                .provider(PaymentProvider.STRIPE)
                .transactionId(invoice.getId())
                .stripeInvoiceId(invoice.getId())
                .stripePaymentIntentId(invoice.getPaymentIntent())
                .amount(BigDecimal.valueOf(invoice.getAmountPaid() == null ? 0L : invoice.getAmountPaid()))
                .currency(invoice.getCurrency() == null ? "EUR" : invoice.getCurrency().toUpperCase())
                .status(TransactionStatus.SUCCEEDED)
                .promoCode(local.getStripeCouponId())
                .build();
        transactionRepository.save(tx);
    }

    private void enrichWithNextInvoiceAmount(Subscription subscription, SubscriptionResponse response) {
        if (subscription.getPlan() == null || subscription.getPlan() == PlanTier.FREE) {
            return;
        }
        long baseAmount = resolveBaseAmount(subscription.getPlan(), subscription.getCurrency());
        response.setNextInvoiceAmount(BigDecimal.valueOf(baseAmount));
        response.setCurrency(subscription.getCurrency());
    }

    private long resolveBaseAmount(PlanTier plan, String currency) {
        boolean eur = "EUR".equalsIgnoreCase(currency);
        return switch (plan) {
            case PREMIUM -> eur ? stripeConfig.getPremiumPriceEur() : stripeConfig.getPremiumPriceXof();
            case PREMIUM_PLUS -> eur ? stripeConfig.getPremiumPlusPriceEur() : stripeConfig.getPremiumPlusPriceXof();
            default -> 0L;
        };
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) return null;
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "incomplete", "incomplete_expired", "past_due", "unpaid" -> SubscriptionStatus.PENDING;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            default -> null;
        };
    }

    private PlanTier planFromStripeStatus(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE ? subscription.getPlan() : PlanTier.FREE;
    }

    private void syncPlanWithAuthService(UUID userId, PlanTier plan) {
        try {
            authClient.updatePlan(PlanUpdateRequest.builder().userId(userId).plan(plan).build());
            log.info("Plan auth-service synchronisé userId={} plan={}", userId, plan);
        } catch (Exception e) {
            log.error("Échec sync plan auth-service userId={} plan={} — retry manuel requis : {}",
                    userId, plan, e.getMessage());
        }
    }
}
