package com.josephyusuf.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "waitlist", schema = "joseph_auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 20)
    private Plan planTier;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String country = "SN";

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "XOF";

    @Column(name = "promo_code_reserved", length = 50)
    private String promoCodeReserved;

    @Column(nullable = false)
    @Builder.Default
    private boolean notified = false;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "converted_user_id")
    private UUID convertedUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
