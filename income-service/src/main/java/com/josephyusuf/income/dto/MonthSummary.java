package com.josephyusuf.income.dto;

import com.josephyusuf.income.entity.MonthStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSummary {

    private UUID userId;
    private int month;
    private int year;
    private BigDecimal totalIncome;
    private BigDecimal averageLast3Months;
    private BigDecimal abundanceThreshold;
    private BigDecimal leanThreshold;
    private MonthStatus status;
    private double percentageVsAverage;
    private int monthsInBaseline; // 0, 1, 2 ou 3
}
