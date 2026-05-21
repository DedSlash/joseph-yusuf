package com.josephyusuf.subscription.repository;

import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<Transaction> findByStripeInvoiceId(String stripeInvoiceId);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:userId IS NULL OR t.userId = :userId)
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllForAdmin(@Param("status") TransactionStatus status,
                                      @Param("userId") UUID userId,
                                      Pageable pageable);
}
