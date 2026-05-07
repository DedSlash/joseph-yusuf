package com.josephyusuf.ruleengine.service;

import com.josephyusuf.ruleengine.client.IncomeClient;
import com.josephyusuf.ruleengine.dto.*;
import com.josephyusuf.ruleengine.entity.RuleType;
import com.josephyusuf.ruleengine.entity.UserRuleConfig;
import com.josephyusuf.ruleengine.exception.RuleNotAccessibleException;
import com.josephyusuf.ruleengine.repository.UserRuleConfigRepository;
import com.josephyusuf.ruleengine.rule.Rule503020Calculator;
import com.josephyusuf.ruleengine.rule.Rule702010Calculator;
import com.josephyusuf.ruleengine.rule.Rule8020Calculator;
import com.josephyusuf.ruleengine.rule.RuleCalculator;
import com.josephyusuf.ruleengine.rule.RuleJosephCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private UserRuleConfigRepository configRepository;

    @Mock
    private IncomeClient incomeClient;

    private RuleEngineService ruleEngineService;

    private UUID userId;
    private UserRuleConfig defaultConfig;

    @BeforeEach
    void setUp() {
        List<RuleCalculator> calculators = List.of(
                new Rule503020Calculator(),
                new Rule8020Calculator(),
                new Rule702010Calculator(),
                new RuleJosephCalculator()
        );

        ruleEngineService = new RuleEngineService(calculators, configRepository, incomeClient);

        userId = UUID.randomUUID();
        defaultConfig = UserRuleConfig.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .activeRule(RuleType.RULE_50_30_20)
                .josephAbundanceSavingsPercent(30)
                .josephLeanSavingsPercent(10)
                .build();
    }

    @Test
    @DisplayName("Plan FREE + RULE_JOSEPH → RuleNotAccessibleException")
    void calculate_freePlanWithRuleJoseph_throwsRuleNotAccessibleException() {
        CalculateRequest request = CalculateRequest.builder()
                .rule(RuleType.RULE_JOSEPH)
                .totalIncome(new BigDecimal("500000"))
                .build();

        assertThatThrownBy(() -> ruleEngineService.calculate(userId, "FREE", request))
                .isInstanceOf(RuleNotAccessibleException.class);
    }

    @Test
    @DisplayName("Plan FREE + RULE_50_30_20 → OK (pas d'exception)")
    void calculate_freePlanWithRule503020_succeeds() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        CalculateRequest request = CalculateRequest.builder()
                .rule(RuleType.RULE_50_30_20)
                .totalIncome(new BigDecimal("500000"))
                .build();

        AllocationResult result = ruleEngineService.calculate(userId, "FREE", request);

        assertThat(result).isNotNull();
        assertThat(result.getRule()).isEqualTo(RuleType.RULE_50_30_20);
        assertThat(result.getAllocations()).hasSize(3);
    }

    @Test
    @DisplayName("Plan PREMIUM + RULE_JOSEPH → OK")
    void calculate_premiumPlanWithRuleJoseph_succeeds() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        MonthSummaryResponse summaryResponse = MonthSummaryResponse.builder()
                .status("NORMAL")
                .totalIncome(new BigDecimal("500000"))
                .build();
        when(incomeClient.getSummary(anyInt(), anyInt())).thenReturn(summaryResponse);

        CalculateRequest request = CalculateRequest.builder()
                .rule(RuleType.RULE_JOSEPH)
                .totalIncome(new BigDecimal("500000"))
                .month(5)
                .year(2026)
                .build();

        AllocationResult result = ruleEngineService.calculate(userId, "PREMIUM", request);

        assertThat(result).isNotNull();
        assertThat(result.getRule()).isEqualTo(RuleType.RULE_JOSEPH);
    }

    @Test
    @DisplayName("Plan PREMIUM_PLUS + toutes les règles → OK")
    void calculate_premiumPlusPlanWithAllRules_succeeds() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        // RULE_50_30_20
        CalculateRequest request503020 = CalculateRequest.builder()
                .rule(RuleType.RULE_50_30_20)
                .totalIncome(new BigDecimal("500000"))
                .build();
        AllocationResult result1 = ruleEngineService.calculate(userId, "PREMIUM_PLUS", request503020);
        assertThat(result1.getRule()).isEqualTo(RuleType.RULE_50_30_20);

        // RULE_80_20
        CalculateRequest request8020 = CalculateRequest.builder()
                .rule(RuleType.RULE_80_20)
                .totalIncome(new BigDecimal("500000"))
                .build();
        AllocationResult result2 = ruleEngineService.calculate(userId, "PREMIUM_PLUS", request8020);
        assertThat(result2.getRule()).isEqualTo(RuleType.RULE_80_20);

        // RULE_70_20_10
        CalculateRequest request702010 = CalculateRequest.builder()
                .rule(RuleType.RULE_70_20_10)
                .totalIncome(new BigDecimal("500000"))
                .build();
        AllocationResult result3 = ruleEngineService.calculate(userId, "PREMIUM_PLUS", request702010);
        assertThat(result3.getRule()).isEqualTo(RuleType.RULE_70_20_10);

        // RULE_JOSEPH
        MonthSummaryResponse summaryResponse = MonthSummaryResponse.builder()
                .status("ABUNDANCE")
                .totalIncome(new BigDecimal("500000"))
                .build();
        when(incomeClient.getSummary(anyInt(), anyInt())).thenReturn(summaryResponse);

        CalculateRequest requestJoseph = CalculateRequest.builder()
                .rule(RuleType.RULE_JOSEPH)
                .totalIncome(new BigDecimal("500000"))
                .month(5)
                .year(2026)
                .build();
        AllocationResult result4 = ruleEngineService.calculate(userId, "PREMIUM_PLUS", requestJoseph);
        assertThat(result4.getRule()).isEqualTo(RuleType.RULE_JOSEPH);
    }

    @Test
    @DisplayName("getAvailableRules(FREE) → seule 50/30/20 déverrouillée, le reste verrouillé")
    void getAvailableRules_freePlan_only503020Unlocked() {
        List<RuleAvailability> rules = ruleEngineService.getAvailableRules("FREE");

        assertThat(rules).hasSize(4);

        RuleAvailability rule503020 = rules.stream()
                .filter(r -> r.getRule() == RuleType.RULE_50_30_20)
                .findFirst().orElseThrow();
        assertThat(rule503020.isLocked()).isFalse();

        RuleAvailability rule8020 = rules.stream()
                .filter(r -> r.getRule() == RuleType.RULE_80_20)
                .findFirst().orElseThrow();
        assertThat(rule8020.isLocked()).isTrue();

        RuleAvailability rule702010 = rules.stream()
                .filter(r -> r.getRule() == RuleType.RULE_70_20_10)
                .findFirst().orElseThrow();
        assertThat(rule702010.isLocked()).isTrue();

        RuleAvailability ruleJoseph = rules.stream()
                .filter(r -> r.getRule() == RuleType.RULE_JOSEPH)
                .findFirst().orElseThrow();
        assertThat(ruleJoseph.isLocked()).isTrue();
    }

    @Test
    @DisplayName("getAvailableRules(PREMIUM) → toutes déverrouillées")
    void getAvailableRules_premiumPlan_allUnlocked() {
        List<RuleAvailability> rules = ruleEngineService.getAvailableRules("PREMIUM");

        assertThat(rules).hasSize(4);

        for (RuleAvailability rule : rules) {
            assertThat(rule.isLocked()).isFalse();
        }
    }

    @Test
    @DisplayName("getAvailableRules(PREMIUM_PLUS) → toutes déverrouillées")
    void getAvailableRules_premiumPlusPlan_allUnlocked() {
        List<RuleAvailability> rules = ruleEngineService.getAvailableRules("PREMIUM_PLUS");

        assertThat(rules).hasSize(4);

        for (RuleAvailability rule : rules) {
            assertThat(rule.isLocked()).isFalse();
        }
    }
}
