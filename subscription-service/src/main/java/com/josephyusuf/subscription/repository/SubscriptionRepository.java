package com.josephyusuf.subscription.repository;

import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserId(UUID userId);

    Optional<Subscription> findByPaddleSubscriptionId(String paddleSubscriptionId);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.status = :status
              AND s.autoRenew = true
              AND s.expiresAt BETWEEN :from AND :to
            """)
    List<Subscription> findActiveExpiringBetween(@Param("status") SubscriptionStatus status,
                                                 @Param("from") Instant from,
                                                 @Param("to") Instant to);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.status = :status
              AND s.expiresAt < :before
            """)
    List<Subscription> findActiveExpiredBefore(@Param("status") SubscriptionStatus status,
                                               @Param("before") Instant before);
}
