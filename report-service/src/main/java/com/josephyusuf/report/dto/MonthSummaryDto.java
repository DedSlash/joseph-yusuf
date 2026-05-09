package com.josephyusuf.report.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthSummaryDto {

    private UUID userId;
    private Integer month;
    private Integer year;
    private BigDecimal totalIncome;
    private BigDecimal averageLast3Months;
    private String status;
    private BigDecimal percentageVsAverage;
}
