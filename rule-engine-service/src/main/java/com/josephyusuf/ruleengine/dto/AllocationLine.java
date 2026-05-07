package com.josephyusuf.ruleengine.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationLine {

    private String category;
    private int percentage;
    private BigDecimal amount;
}
