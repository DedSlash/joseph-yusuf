package com.josephyusuf.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private UUID id;
    private UUID userId;
    private String provider;
    private String providerTransactionId;
    private BigDecimal amount;
    private String currency;
    private String plan;
    private String status;
    private Instant createdAt;
}
