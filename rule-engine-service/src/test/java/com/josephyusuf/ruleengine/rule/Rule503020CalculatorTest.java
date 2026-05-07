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

class Rule503020CalculatorTest {

    private Rule503020Calculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new Rule503020Calculator();
    }

    @Test
    @DisplayName("getSupportedRule retourne RULE_50_30_20")
    void getSupportedRule_returnsCorrectType() {
        assertThat(calculator.getSupportedRule()).isEqualTo(RuleType.RULE_50_30_20);
    }

    @Test
    @DisplayName("500000 → [250000, 150000, 100000]")
    void calculate_withIncome500000_returnsCorrectAllocations() {
        BigDecimal income = new BigDecimal("500000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        assertThat(result.getRule()).isEqualTo(RuleType.RULE_50_30_20);
        assertThat(result.getTotalIncome()).isEqualByComparingTo(income);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getCategory()).isEqualTo("Besoins essentiels");
        assertThat(allocations.get(0).getPercentage()).isEqualTo(50);
        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("250000.00"));

        assertThat(allocations.get(1).getCategory()).isEqualTo("Envies / loisirs");
        assertThat(allocations.get(1).getPercentage()).isEqualTo(30);
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("150000.00"));

        assertThat(allocations.get(2).getCategory()).isEqualTo("Épargne / dettes");
        assertThat(allocations.get(2).getPercentage()).isEqualTo(20);
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("0 → [0, 0, 0]")
    void calculate_withZeroIncome_returnsZeroAllocations() {
        BigDecimal income = BigDecimal.ZERO;

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("333333 → vérifie arrondi HALF_UP (166666.50, 99999.90, 66666.60)")
    void calculate_withOddIncome_verifiesRoundingHalfUp() {
        BigDecimal income = new BigDecimal("333333");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        List<AllocationLine> allocations = result.getAllocations();
        assertThat(allocations).hasSize(3);

        assertThat(allocations.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("166666.50"));
        assertThat(allocations.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("99999.90"));
        assertThat(allocations.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("66666.60"));
    }

    @Test
    @DisplayName("Total des pourcentages = 100")
    void calculate_totalPercentagesEquals100() {
        BigDecimal income = new BigDecimal("500000");

        AllocationResult result = calculator.calculate(income, null, 30, 10);

        int totalPercentage = result.getAllocations().stream()
                .mapToInt(AllocationLine::getPercentage)
                .sum();

        assertThat(totalPercentage).isEqualTo(100);
    }
}
