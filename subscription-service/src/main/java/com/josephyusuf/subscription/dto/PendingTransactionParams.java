package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PendingTransactionParams {

    private final UUID userId;
    private final PlanTier plan;
    private final PaymentProvider provider;
    private final String externalTxId;
    private final String providerToken;
    private final BigDecimal amount;
    private final String currency;
    private final String promoCode;
    private final Integer discountPercent;
    private final BigDecimal originalAmount;
    private final boolean couponLifetime;
}
