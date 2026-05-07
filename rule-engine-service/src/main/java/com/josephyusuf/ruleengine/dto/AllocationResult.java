package com.josephyusuf.ruleengine.dto;

import com.josephyusuf.ruleengine.entity.RuleType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationResult {

    private RuleType rule;
    private BigDecimal totalIncome;
    private String monthStatus;
    private String message;
    private List<AllocationLine> allocations;
}
