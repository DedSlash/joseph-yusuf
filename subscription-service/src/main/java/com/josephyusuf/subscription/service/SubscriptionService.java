package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.PlanUpdateRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionMapper mapper;
    private final AuthClient authClient;
    private SubscriptionService self;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               TransactionRepository transactionRepository,
                               SubscriptionMapper mapper,
                               AuthClient authClient,
                               @Lazy SubscriptionService self) {
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.authClient = authClient;
        this.self = self;
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

    @Transactional
    public Subscription activateAfterPayment(UUID userId, PlanTier plan, PaymentProvider provider,
                                             String externalTxId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .map(existing -> {
                    existing.setPlan(plan);
                    existing.setStatus(SubscriptionStatus.ACTIVE);
                    existing.setProvider(provider);
                    existing.setStartedAt(Instant.now());
                    existing.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
                    existing.setCancelledAt(null);
                    return existing;
                })
                .orElseGet(() -> Subscription.builder()
                        .userId(userId)
                        .plan(plan)
                        .status(SubscriptionStatus.ACTIVE)
                        .provider(provider)
                        .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
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
        log.info("Subscription activée userId={} plan={} provider={}", userId, plan, provider);
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

    public SubscriptionResponse getCurrent(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Aucun abonnement trouvé pour userId=" + userId));
        return mapper.toResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse confirmStripePayment(UUID userId, String paymentIntentId) {
        // Vérifie le statut réel du PaymentIntent directement auprès de Stripe
        com.stripe.model.PaymentIntent intent;
        try {
            intent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
        } catch (com.stripe.exception.StripeException e) {
            throw new com.josephyusuf.subscription.exception.PaymentException(
                    "Impossible de vérifier le paiement : " + e.getMessage(), e);
        }

        if (!"succeeded".equals(intent.getStatus())) {
            throw new com.josephyusuf.subscription.exception.PaymentException(
                    "Paiement non confirmé — statut : " + intent.getStatus());
        }

        // Vérifie que ce PaymentIntent appartient bien à cet utilisateur
        String metaUserId = intent.getMetadata().get("userId");
        if (!userId.toString().equals(metaUserId)) {
            throw new com.josephyusuf.subscription.exception.PaymentException(
                    "Paiement non autorisé");
        }

        PlanTier plan = PlanTier.valueOf(intent.getMetadata().get("plan"));
        Subscription sub = self.activateAfterPayment(userId, plan, PaymentProvider.STRIPE, paymentIntentId);
        return mapper.toResponse(sub);
    }

    @Transactional
    public SubscriptionResponse setAutoRenew(UUID userId, boolean autoRenew) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Aucun abonnement actif"));
        sub.setAutoRenew(autoRenew);
        Subscription saved = subscriptionRepository.save(sub);
        log.info("autoRenew={} userId={}", autoRenew, userId);
        return mapper.toResponse(saved);
    }

    @Transactional
    public Subscription cancel(UUID userId) {
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Aucun abonnement actif"));
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new InvalidPlanException("Abonnement déjà annulé");
        }
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(Instant.now());
        Subscription saved = subscriptionRepository.save(sub);
        syncPlanWithAuthService(userId, PlanTier.FREE);
        log.info("Abonnement annulé userId={}", userId);
        return saved;
    }

    public Page<TransactionResponse> getHistory(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toResponse);
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
