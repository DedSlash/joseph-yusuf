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
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.mapper.SubscriptionMapper;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.josephyusuf.subscription.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final long PREMIUM_PRICE_XOF = 2990L;
    private static final long PREMIUM_PLUS_PRICE_XOF = 5990L;

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionMapper mapper;
    private final AuthClient authClient;
    private final PaddleService paddleService;

    /**
     * Activation après paiement réussi (PayTech IPN, ou activation manuelle admin).
     * Lit la Transaction associée à externalTxId pour persister sur la subscription
     * le coupon appliqué et son flag {@code lifetime} (utilisé au renouvellement).
     */
    @Transactional
    public Subscription activateAfterPayment(UUID userId, PlanTier plan, PaymentProvider provider,
                                             String externalTxId) {
        Instant now = Instant.now();

        Transaction sourceTx = externalTxId != null
                ? transactionRepository.findByTransactionId(externalTxId).orElse(null)
                : null;
        String couponApplied = sourceTx != null ? sourceTx.getPromoCode() : null;
        boolean couponLifetime = sourceTx != null && sourceTx.isCouponLifetime();
        int months = sourceTx != null && sourceTx.getMonthsCount() > 0
                ? sourceTx.getMonthsCount() : 1;
        Instant expiry = now.plus(30L * months, ChronoUnit.DAYS);

        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .map(existing -> {
                    existing.setPlan(plan);
                    existing.setStatus(SubscriptionStatus.ACTIVE);
                    existing.setProvider(provider);
                    existing.setStartedAt(now);
                    existing.setExpiresAt(expiry);
                    existing.setCurrentPeriodStart(now);
                    existing.setCurrentPeriodEnd(expiry);
                    existing.setCancelAtPeriodEnd(false);
                    existing.setCancelledAt(null);
                    existing.setAutoRenew(true);
                    if (couponApplied != null) {
                        existing.setCouponApplied(couponApplied);
                        existing.setCouponLifetime(couponLifetime);
                    }
                    return existing;
                })
                .orElseGet(() -> Subscription.builder()
                        .userId(userId)
                        .plan(plan)
                        .status(SubscriptionStatus.ACTIVE)
                        .provider(provider)
                        .startedAt(now)
                        .expiresAt(expiry)
                        .currentPeriodStart(now)
                        .currentPeriodEnd(expiry)
                        .autoRenew(true)
                        .couponApplied(couponApplied)
                        .couponLifetime(couponLifetime)
                        .build());

        Subscription saved = subscriptionRepository.save(subscription);

        if (sourceTx != null) {
            sourceTx.setStatus(TransactionStatus.SUCCEEDED);
            sourceTx.setSubscriptionId(saved.getId());
            transactionRepository.save(sourceTx);
        }

        syncPlanWithAuthService(userId, plan);
        log.info("Subscription activée userId={} plan={} provider={} coupon={} lifetime={}",
                userId, plan, provider, couponApplied, couponLifetime);
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

    /**
     * Reçoit un IPN refund_complete : marque la transaction REFUNDED puis downgrade
     * l'abonnement de l'utilisateur en FREE (statut CANCELLED). Sans cette
     * descente, l'utilisateur garderait son accès premium gratuitement après le
     * remboursement effectif.
     */
    @Transactional
    public void markRefundAndDowngrade(UUID userId, String externalTxId) {
        markTransactionRefunded(externalTxId);

        subscriptionRepository.findByUserId(userId).ifPresent(sub -> {
            sub.setPlan(PlanTier.FREE);
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setCancelledAt(Instant.now());
            sub.setExpiresAt(Instant.now());
            sub.setAutoRenew(false);
            subscriptionRepository.save(sub);
        });

        syncPlanWithAuthService(userId, PlanTier.FREE);
        log.info("Refund + downgrade FREE appliqués userId={} tx={}", userId, externalTxId);
    }

    @Transactional
    public Transaction recordPendingTransaction(PendingTransactionParams params) {
        if (params.getPlan() == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de transaction");
        }
        int months = params.getMonthsCount() < 1 ? 1 : params.getMonthsCount();
        Transaction tx = Transaction.builder()
                .userId(params.getUserId())
                .plan(params.getPlan())
                .provider(params.getProvider())
                .transactionId(params.getExternalTxId())
                .providerToken(params.getProviderToken())
                .amount(params.getAmount())
                .currency(params.getCurrency())
                .status(TransactionStatus.PENDING)
                .promoCode(params.getPromoCode())
                .discountPercent(params.getDiscountPercent())
                .originalAmount(params.getOriginalAmount())
                .couponLifetime(params.isCouponLifetime())
                .monthsCount(months)
                .build();
        return transactionRepository.save(tx);
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
     * Annulation : l'utilisateur conserve l'accès jusqu'à expiresAt puis l'abonnement
     * expire naturellement.
     *
     * <p>Pour les abonnements Paddle, on appelle aussi
     * {@code Paddle /subscriptions/{id}/cancel} (effective_from = next_billing_period)
     * pour stopper les prélèvements futurs. Sans cet appel, Paddle continuerait
     * de débiter la carte au prochain cycle. Si l'API Paddle échoue, on remonte
     * l'erreur au lieu de marquer en local pour éviter une désynchronisation
     * silencieuse ("annulé chez nous mais toujours débité chez Paddle").</p>
     *
     * <p>Pour les paiements mobile money (PayTech), aucun appel upstream
     * n'est nécessaire : il n'y a pas de prélèvement automatique côté PayTech.</p>
     */
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID userId, boolean immediately) {
        Subscription local = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Aucun abonnement actif"));

        if (local.getProvider() == PaymentProvider.PADDLE
                && local.getPaddleSubscriptionId() != null
                && !local.getPaddleSubscriptionId().isBlank()) {
            try {
                paddleService.cancelSubscription(local.getPaddleSubscriptionId());
                log.info("Paddle subscription annulée upstream userId={} paddleId={}",
                        userId, local.getPaddleSubscriptionId());
            } catch (Exception e) {
                log.error("Échec annulation Paddle userId={} paddleId={} : {}",
                        userId, local.getPaddleSubscriptionId(), e.getMessage());
                throw new PaymentException(
                        "Impossible d'annuler l'abonnement Paddle. Réessayez dans quelques instants.");
            }
        }

        if (immediately) {
            local.setStatus(SubscriptionStatus.CANCELLED);
            local.setCancelledAt(Instant.now());
            local.setExpiresAt(Instant.now());
        } else {
            local.setCancelAtPeriodEnd(true);
            local.setCancelledAt(Instant.now());
        }
        local.setAutoRenew(false);
        Subscription saved = subscriptionRepository.save(local);
        log.info("Subscription annulée userId={} immediately={} provider={}",
                userId, immediately, local.getProvider());
        return mapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionResponse setAutoRenew(UUID userId, boolean autoRenew) {
        Subscription local = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionNotFoundException("Aucun abonnement actif"));
        local.setAutoRenew(autoRenew);
        local.setCancelAtPeriodEnd(!autoRenew);
        Subscription saved = subscriptionRepository.save(local);
        log.info("autoRenew={} userId={}", autoRenew, userId);
        return mapper.toResponse(saved);
    }

    public Page<TransactionResponse> getHistory(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toResponse);
    }

    private void enrichWithNextInvoiceAmount(Subscription subscription, SubscriptionResponse response) {
        if (subscription.getPlan() == null || subscription.getPlan() == PlanTier.FREE) {
            return;
        }
        long baseAmount = switch (subscription.getPlan()) {
            case PREMIUM -> PREMIUM_PRICE_XOF;
            case PREMIUM_PLUS -> PREMIUM_PLUS_PRICE_XOF;
            default -> 0L;
        };
        response.setNextInvoiceAmount(BigDecimal.valueOf(baseAmount));
        response.setCurrency(subscription.getCurrency() != null ? subscription.getCurrency() : "XOF");
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
