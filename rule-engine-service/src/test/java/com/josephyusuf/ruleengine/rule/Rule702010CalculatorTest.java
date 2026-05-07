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

class Rule702010CalculatorTest {

    private Rule702010Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Rule702010Calculator();
    }

    @Test
    @DisplayName("getSupportedRule retourne RULE_70_20_10")
    void getSupportedRule_returnsCorrectType() {
        assertThat(calculator.getSupportedRule()).isEqualTo(RuleType.RULE_70_20_10);
    }

    @Test
    @DisplayName("700000 → [490000, 140000, 70000]")
    void calculate_withIncome700000_returnsCorrectAllocations() {
        BigDecimal income = new BigDecimal("700000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        assertThat(result.getRule()).isEqualTo(RuleType.RULE_70_20_10);
        assertThat(result.getTotalIncome()).isEqualByComparingTo(income);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getCategory()).isEqualTo("Dépenses courantes");
        assertThat(allocations.get(0).getPercentage()).isEqualTo(70);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("490000.00"));

        assertThat(allocations.get(1).getCategory()).isEqualTo("Épargne");
        assertThat(allocations.get(1).getPercentage()).isEqualTo(20);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("140000.00"));

        assertThat(allocations.get(2).getCategory()).isEqualTo("Don / Zakat / Dîme");
        assertThat(allocations.get(2).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("70000.00"));
    }

    @Test
    @DisplayName("Total des pourcentages = 100")
    void calculate_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("700000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();

        assertThat(totalPercentage).isEqualTo(100);
    }
}
