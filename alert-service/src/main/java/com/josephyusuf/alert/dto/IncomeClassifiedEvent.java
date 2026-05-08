package com.josephyusuf.alert.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeClassifiedEvent {

    private UUID userId;
    private int month;
    private int year;
    private BigDecimal totalIncome;
    private BigDecimal averageLast3Months;
    private String status;
    private double percentageVsAverage;
    private Instant occurredAt;
}
