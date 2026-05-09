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
public class TransactionResponse {

    private UUID id;
    private PlanTier plan;
    private PaymentProvider provider;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private Instant createdAt;
}
