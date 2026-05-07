package com.josephyusuf.ruleengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_rule_configs", schema = "joseph_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRuleConfig {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_rule", nullable = false, length = 20)
    @Builder.Default
    private RuleType activeRule = RuleType.RULE_50_30_20;

    @Column(name = "joseph_abundance_savings_percent", nullable = false)
    @Builder.Default
    private int josephAbundanceSavingsPercent = 30;

    @Column(name = "joseph_lean_savings_percent", nullable = false)
    @Builder.Default
    private int josephLeanSavingsPercent = 10;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
