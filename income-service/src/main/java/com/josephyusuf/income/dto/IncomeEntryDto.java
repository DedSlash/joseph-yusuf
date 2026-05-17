package com.josephyusuf.income.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeEntryDto {

    private UUID id;
    private UUID incomeSourceId;
    private String incomeSourceName;
    private BigDecimal amount;
    private BigDecimal amountXof;
    private String currency;
    private int month;
    private int year;
    private String note;
    private Instant createdAt;
}
