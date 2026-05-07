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

class RuleJosephCalculatorTest {

    private RuleJosephCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RuleJosephCalculator();
    }

    @Test
    @DisplayName("getSupportedRule retourne RULE_JOSEPH")
    void getSupportedRule_returnsCorrectType() {
        assertThat(calculator.getSupportedRule()).isEqualTo(RuleType.RULE_JOSEPH);
    }

    @Test
    @DisplayName("ABUNDANCE avec abundanceSavingsPercent=30 → [60%, 30%, 10%]")
    void calculate_abundance30_returnsCorrectAllocations() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "ABUNDANCE", 30, 10);

        assertThat(result.getRule()).isEqualTo(RuleType.RULE_JOSEPH);
        assertThat(result.getMonthStatus()).isEqualTo("ABUNDANCE");

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getCategory()).isEqualTo("Dépenses courantes");
        assertThat(allocations.get(0).getPercentage()).isEqualTo(60);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("600000.00"));

        assertThat(allocations.get(1).getCategory()).isEqualTo("Épargne");
        assertThat(allocations.get(1).getPercentage()).isEqualTo(30);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("300000.00"));

        assertThat(allocations.get(2).getCategory()).isEqualTo("Don / Zakat / Dîme");
        assertThat(allocations.get(2).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("ABUNDANCE avec abundanceSavingsPercent=35 → [55%, 35%, 10%]")
    void calculate_abundance35_returnsCorrectAllocations() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "ABUNDANCE", 35, 10);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getPercentage()).isEqualTo(55);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("550000.00"));

        assertThat(allocations.get(1).getPercentage()).isEqualTo(35);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("350000.00"));

        assertThat(allocations.get(2).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("LEAN avec leanSavingsPercent=10 → [80%, 10%, 10%]")
    void calculate_lean10_returnsCorrectAllocations() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "LEAN", 30, 10);

        assertThat(result.getMonthStatus()).isEqualTo("LEAN");

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getPercentage()).isEqualTo(80);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("800000.00"));

        assertThat(allocations.get(1).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));

        assertThat(allocations.get(2).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("NORMAL → [70%, 20%, 10%]")
    void calculate_normal_returnsDefaultAllocations() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "NORMAL", 30, 10);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getPercentage()).isEqualTo(70);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("700000.00"));

        assertThat(allocations.get(1).getPercentage()).isEqualTo(20);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));

        assertThat(allocations.get(2).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("null monthStatus → traité comme NORMAL [70%, 20%, 10%]")
    void calculate_nullMonthStatus_treatedAsNormal() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getPercentage()).isEqualTo(70);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("700000.00"));

        assertThat(allocations.get(1).getPercentage()).isEqualTo(20);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));

        assertThat(allocations.get(2).getPercentage()).isEqualTo(10);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("Total pourcentages = 100 pour ABUNDANCE")
    void calculate_abundance_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "ABUNDANCE", 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();
        assertThat(totalPercentage).isEqualTo(100);
    }

    @Test
    @DisplayName("Total pourcentages = 100 pour LEAN")
    void calculate_lean_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "LEAN", 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();
        assertThat(totalPercentage).isEqualTo(100);
    }

    @Test
    @DisplayName("Total pourcentages = 100 pour NORMAL")
    void calculate_normal_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "NORMAL", 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();
        assertThat(totalPercentage).isEqualTo(100);
    }

    @Test
    @DisplayName("Total pourcentages = 100 pour null monthStatus")
    void calculate_nullStatus_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();
        assertThat(totalPercentage).isEqualTo(100);
    }

    @Test
    @DisplayName("ABUNDANCE message contient 'abondance'")
    void calculate_abundance_messageContainsAbondance() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "ABUNDANCE", 30, 10);

        assertThat(result.getMessage()).containsIgnoringCase("abondance");
    }

    @Test
    @DisplayName("LEAN message contient 'difficile'")
    void calculate_lean_messageContainsDifficile() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "LEAN", 30, 10);

        assertThat(result.getMessage()).containsIgnoringCase("difficile");
    }

    @Test
    @DisplayName("NORMAL → message est null")
    void calculate_normal_messageIsNull() {
        BigDecimal income = new BigDecimal("1000000");

        AllocationResult result = calculator.calculate(income, "NORMAL", 30, 10);

        assertThat(result.getMessage()).isNull();
    }
}
