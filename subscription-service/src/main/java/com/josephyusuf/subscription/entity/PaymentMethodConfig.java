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

    /**
     * Backend qui gère ce moyen de paiement côté serveur.
     * Valeurs : "PAYTECH" (Wave/Orange Money/Free Money) ou "PADDLE" (carte bancaire).
     * Le frontend route l'appel /create selon cette valeur.
     */
    @Column(name = "routing", nullable = false, length = 20)
    @Builder.Default
    private String routing = "PAYTECH";

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
