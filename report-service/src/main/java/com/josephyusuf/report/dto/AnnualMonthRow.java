package com.josephyusuf.report.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualMonthRow {

    private Integer month;
    private BigDecimal totalIncome;
    private String status;
}
