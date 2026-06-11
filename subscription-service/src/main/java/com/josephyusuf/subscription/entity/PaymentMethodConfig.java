package com.josephyusuf.subscription.entity;

import com.josephyusuf.subscription.enums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "payment_method_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 50)
    private PaymentProvider provider;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 99;

    @Column(name = "paytech_method_code", length = 50)
    private String paytechMethodCode;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
