package com.josephyusuf.report.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualReportData {

    private UUID userId;
    private Integer year;
    private BigDecimal totalAnnualIncome;
    private Integer abundanceMonths;
    private Integer leanMonths;
    private Integer normalMonths;
    private List<AnnualMonthRow> rows;
}
