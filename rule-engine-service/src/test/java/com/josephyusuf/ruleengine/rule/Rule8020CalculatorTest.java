package com.josephyusuf.ruleengine.rule;

import com.josephyusuf.ruleengine.dto.AllocationLine;
import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.entity.RuleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Rule8020CalculatorTest {

    private Rule8020Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Rule8020Calculator();
    }

    @Test
    @DisplayName("getSupportedRule retourne RULE_80_20")
    void getSupportedRule_returnsCorrectType() {
        assertThat(calculator.getSupportedRule()).isEqualTo(RuleType.RULE_80_20);
    }

    @Test
    @DisplayName("1000000 → [800000, 200000]")
    void calculate_withIncome1000000_returnsCorrectAllocations() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        assertThat(result.getRule()).isEqualTo(RuleType.RULE_80_20);
        assertThat(result.getTotalIncome()).isEqualByComparingTo(income);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(2);

        assertThat(allocations.get(0).getCategory()).isEqualTo("Dépenses (vie courante)");
        assertThat(allocations.get(0).getPercentage()).isEqualTo(80);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("800000.00"));

        assertThat(allocations.get(1).getCategory()).isEqualTo("Épargne / investissement");
        assertThat(allocations.get(1).getPercentage()).isEqualTo(20);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
    }

    @Test
    @DisplayName("0 → [0, 0]")
    void calculate_withZeroIncome_returnsZeroAllocations() {
        BigDecimal income = BigDecimal.ZERO;

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(2);

        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Total des pourcentages = 100")
    void calculate_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();

        assertThat(totalPercentage).isEqualTo(100);
    }
}
