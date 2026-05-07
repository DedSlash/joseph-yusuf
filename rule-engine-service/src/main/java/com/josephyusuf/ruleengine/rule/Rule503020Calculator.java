package com.josephyusuf.ruleengine.rule;

import com.josephyusuf.ruleengine.dto.AllocationLine;
import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.entity.RuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Rule503020Calculator implements RuleCalculator {

    @Override
    public RuleType getSupportedRule() {
        return RuleType.RULE_50_30_20;
    }

    @Override
    public AllocationResult calculate(BigDecimal totalIncome, String monthStatus, int abundanceSavingsPercent, int leanSavingsPercent) {
        List<AllocationLine> allocations = List.of(
                AllocationLine.builder()
                        .category("Besoins essentiels")
                        .percentage(50)
                        .amount(totalIncome.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP))
                        .build(),
                AllocationLine.builder()
                        .category("Envies / loisirs")
                        .percentage(30)
                        .amount(totalIncome.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP))
                        .build(),
                AllocationLine.builder()
                        .category("Épargne / dettes")
                        .percentage(20)
                        .amount(totalIncome.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP))
                        .build()
        );

        return AllocationResult.builder()
                .rule(RuleType.RULE_50_30_20)
                .totalIncome(totalIncome)
                .allocations(allocations)
                .build();
    }
}
