package com.josephyusuf.report.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationLineDto {

    private String label;
    private BigDecimal amount;
    private BigDecimal percentage;
}
