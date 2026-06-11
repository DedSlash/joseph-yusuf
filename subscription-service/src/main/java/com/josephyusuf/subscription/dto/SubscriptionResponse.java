package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    private UUID id;
    private UUID userId;
    private PlanTier plan;
    private SubscriptionStatus status;
    private PaymentProvider provider;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant cancelledAt;
    private boolean autoRenew;

    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private boolean cancelAtPeriodEnd;

    private BigDecimal nextInvoiceAmount;
    private String currency;
}
