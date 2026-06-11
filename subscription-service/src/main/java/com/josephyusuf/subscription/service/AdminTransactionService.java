package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.AdminPageResponse;
import com.josephyusuf.subscription.dto.AdminTransactionDto;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private final TransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminTransactionDto> list(int page, int size, String status, UUID userId) {
        Pageable pageable = PageRequest.of(page, size);
        TransactionStatus statusFilter = parseStatus(status);
        Page<Transaction> result = transactionRepository.findAllForAdmin(statusFilter, userId, pageable);

        List<AdminTransactionDto> content = result.getContent().stream()
                .map(this::toDto)
                .toList();

        return AdminPageResponse.<AdminTransactionDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminTransactionDto get(UUID id) {
        return toDto(findById(id));
    }

    /**
     * Marquage local d'un remboursement. L'administrateur doit avoir préalablement
     * effectué le remboursement réel sur le dashboard PayTech ; cet endpoint sert
     * uniquement à refléter cet état côté Joseph·Yusuf.
     */
    @Transactional
    public AdminTransactionDto refund(UUID id) {
        Transaction transaction = findById(id);
        if (transaction.getStatus() != TransactionStatus.SUCCEEDED) {
            throw new PaymentException("Seules les transactions SUCCEEDED peuvent être remboursées");
        }
        transaction.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(transaction);
        log.info("Transaction marquée REFUNDED localement id={} provider={}", id, transaction.getProvider());
        return toDto(transaction);
    }

    @Transactional
    public AdminTransactionDto cancel(UUID id) {
        Transaction transaction = findById(id);
        if (transaction.getStatus() != TransactionStatus.PENDING
                && transaction.getStatus() != TransactionStatus.FAILED) {
            throw new PaymentException("Seules les transactions PENDING ou FAILED peuvent être annulées");
        }
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setFailureReason("Annulée manuellement par l'administrateur");
        transactionRepository.save(transaction);
        log.info("Transaction annulée manuellement id={}", id);
        return toDto(transaction);
    }

    @Transactional
    public AdminTransactionDto forceActivate(UUID id) {
        Transaction transaction = findById(id);
        if (transaction.getStatus() == TransactionStatus.REFUNDED
                || transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new PaymentException("Cette transaction ne peut pas être activée dans son état actuel.");
        }
        subscriptionService.activateAfterPayment(
                transaction.getUserId(),
                transaction.getPlan(),
                transaction.getProvider(),
                transaction.getTransactionId());
        Transaction updated = findById(id);
        log.info("Abonnement activé manuellement pour userId={} plan={}", transaction.getUserId(), transaction.getPlan());
        return toDto(updated);
    }

    private Transaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Transaction introuvable."));
    }

    private AdminTransactionDto toDto(Transaction tx) {
        return AdminTransactionDto.builder()
                .id(tx.getId())
                .userId(tx.getUserId())
                .provider(tx.getProvider())
                .providerTransactionId(tx.getTransactionId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .plan(tx.getPlan())
                .status(tx.getStatus())
                .createdAt(tx.getCreatedAt())
                .promoCode(tx.getPromoCode())
                .discountPercent(tx.getDiscountPercent())
                .originalAmount(tx.getOriginalAmount())
                .build();
    }

    private TransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return TransactionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
