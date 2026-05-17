package com.josephyusuf.ruleengine.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSummaryResponse {

    private UUID userId;
    private int month;
    private int year;
    private BigDecimal totalIncome;
    private BigDecimal averageLast3Months;
    private BigDecimal abundanceThreshold;
    private BigDecimal leanThreshold;
    private String status;
    private double percentageVsAverage;
    private int monthsInBaseline;
}
