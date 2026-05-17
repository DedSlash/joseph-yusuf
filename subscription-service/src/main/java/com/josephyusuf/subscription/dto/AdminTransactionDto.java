package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTransactionDto {

    private UUID id;
    private UUID userId;
    private PaymentProvider provider;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private PlanTier plan;
    private TransactionStatus status;
    private Instant createdAt;
    private String promoCode;
    private Integer discountPercent;
    private java.math.BigDecimal originalAmount;
}
