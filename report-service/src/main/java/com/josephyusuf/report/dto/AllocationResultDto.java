package com.josephyusuf.report.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationResultDto {

    private String rule;
    private BigDecimal totalIncome;
    private String monthStatus;
    private String message;
    private List<AllocationLineDto> allocations;
}
