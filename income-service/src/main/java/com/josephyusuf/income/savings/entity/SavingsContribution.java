package com.josephyusuf.income.savings.entity;

import com.josephyusuf.income.entity.MonthStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @ExtractToSavingsService — vivra dans savings-service.savings_contributions après extraction.
 */
@Entity
@Table(name = "savings_contributions", schema = "joseph_income")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsContribution {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SavingsContributionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "joseph_status", length = 20)
    private MonthStatus josephStatus;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
