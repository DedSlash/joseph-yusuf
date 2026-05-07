package com.josephyusuf.ruleengine.rule;

import com.josephyusuf.ruleengine.dto.AllocationLine;
import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.entity.RuleType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class RuleJosephCalculator implements RuleCalculator {

    @Override
    public RuleType getSupportedRule() {
        return RuleType.RULE_JOSEPH;
    }

    @Override
    public AllocationResult calculate(BigDecimal totalIncome, String monthStatus, int abundanceSavingsPercent, int leanSavingsPercent) {
        int expensePercent;
        int savingsPercent;
        int donPercent = 10;
        String message = null;

        if ("ABUNDANCE".equals(monthStatus)) {
            savingsPercent = abundanceSavingsPercent;
            expensePercent = 100 - savingsPercent - donPercent;
            message = "Mois d'abondance — épargne " + (savingsPercent - 20) + "% de plus ce mois";
        } else if ("LEAN".equals(monthStatus)) {
            savingsPercent = leanSavingsPercent;
            expensePercent = 100 - savingsPercent - donPercent;
            message = "Mois difficile — puise intelligemment dans ton épargne";
        } else {
            expensePercent = 70;
            savingsPercent = 20;
        }

        List<AllocationLine> allocations = List.of(
                AllocationLine.builder()
                        .category("Dépenses courantes")
                        .percentage(expensePercent)
                        .amount(totalIncome.multiply(new BigDecimal(expensePercent))
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                        .build(),
                AllocationLine.builder()
                        .category("Épargne")
                        .percentage(savingsPercent)
                        .amount(totalIncome.multiply(new BigDecimal(savingsPercent))
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                        .build(),
                AllocationLine.builder()
                        .category("Don / Zakat / Dîme")
                        .percentage(donPercent)
                        .amount(totalIncome.multiply(new BigDecimal(donPercent))
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                        .build()
        );

        return AllocationResult.builder()
                .rule(RuleType.RULE_JOSEPH)
                .totalIncome(totalIncome)
                .monthStatus(monthStatus)
                .message(message)
                .allocations(allocations)
                .build();
    }
}
