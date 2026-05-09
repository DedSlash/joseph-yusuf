package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.AdminPageResponse;
import com.josephyusuf.subscription.dto.AdminTransactionDto;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.repository.TransactionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
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

    @Transactional
    public AdminTransactionDto refund(UUID id) {
        Transaction transaction = findById(id);
        if (transaction.getStatus() != TransactionStatus.SUCCEEDED) {
            throw new PaymentException("Seules les transactions SUCCEEDED peuvent être remboursées");
        }
        try {
            Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(transaction.getTransactionId())
                    .build());
            log.info("Stripe refund créé pour PI={}", transaction.getTransactionId());
        } catch (StripeException e) {
            throw new PaymentException("Échec remboursement Stripe : " + e.getMessage(), e);
        }

        transaction.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(transaction);
        return toDto(transaction);
    }

    private Transaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException("Transaction introuvable : " + id));
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
