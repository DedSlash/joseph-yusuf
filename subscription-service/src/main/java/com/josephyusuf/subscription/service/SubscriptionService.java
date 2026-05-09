package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AuthClient;
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

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionMapper mapper;
    private final AuthClient authClient;

    @Transactional
    public Transaction recordPendingTransaction(UUID userId, PlanTier plan, PaymentProvider provider,
                                                String externalTxId, BigDecimal amount, String currency) {
        if (plan == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de transaction");
        }
        Transaction tx = Transaction.builder()
                .userId(userId)
                .plan(plan)
                .provider(provider)
                .transactionId(externalTxId)
                .amount(amount)
                .currency(currency)
                .status(TransactionStatus.PENDING)
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
