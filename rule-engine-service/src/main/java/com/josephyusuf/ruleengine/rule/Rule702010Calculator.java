package com.josephyusuf.ruleengine.rule;

import com.josephyusuf.ruleengine.dto.AllocationLine;
import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.entity.RuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Rule702010Calculator implements RuleCalculator {

    @Override
    public RuleType getSupportedRule() {
        return RuleType.RULE_70_20_10;
    }

    @Override
    public AllocationResult calculate(BigDecimal totalIncome, String monthStatus, int abundanceSavingsPercent, int leanSavingsPercent) {
        List<AllocationLine> allocations = List.of(
                AllocationLine.builder()
                        .category("Dépenses courantes")
                        .percentage(70)
                        .amount(totalIncome.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP))
                        .build(),
                AllocationLine.builder()
                        .category("Épargne")
                        .percentage(20)
                        .amount(totalIncome.multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP))
                        .build(),
                AllocationLine.builder()
                        .category("Don / Zakat / Dîme")
                        .percentage(10)
                        .amount(totalIncome.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP))
                        .build()
        );

        return AllocationResult.builder()
                .rule(RuleType.RULE_70_20_10)
                .totalIncome(totalIncome)
                .allocations(allocations)
                .build();
    }
}
