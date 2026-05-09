package com.josephyusuf.ruleengine.service;

import com.josephyusuf.ruleengine.client.IncomeClient;
import com.josephyusuf.ruleengine.dto.*;
import com.josephyusuf.ruleengine.entity.RuleType;
import com.josephyusuf.ruleengine.entity.UserRuleConfig;
import com.josephyusuf.ruleengine.exception.IncomeDataNotFoundException;
import com.josephyusuf.ruleengine.producer.RuleEventProducer;
import com.josephyusuf.ruleengine.repository.UserRuleConfigRepository;
import com.josephyusuf.ruleengine.rule.*;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceAdditionalTest {

    @Mock
    private UserRuleConfigRepository configRepository;

    @Mock
    private IncomeClient incomeClient;

    @Mock
    private RuleEventProducer ruleEventProducer;

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

        ruleEngineService = new RuleEngineService(calculators, configRepository, incomeClient, ruleEventProducer);
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
    @DisplayName("calculateCurrent - throws IncomeDataNotFoundException when incomeClient fails")
    void calculateCurrent_clientException_throwsIncomeDataNotFound() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));
        when(incomeClient.getSummary(anyInt(), anyInt())).thenThrow(new RuntimeException("connection failed"));

        assertThatThrownBy(() -> ruleEngineService.calculateCurrent(userId, "FREE"))
                .isInstanceOf(IncomeDataNotFoundException.class)
                .hasMessageContaining("Pas de données de revenus");
    }

    @Test
    @DisplayName("calculateCurrent - throws IncomeDataNotFoundException when totalIncome is null")
    void calculateCurrent_nullIncome_throwsIncomeDataNotFound() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        MonthSummaryResponse summary = MonthSummaryResponse.builder()
                .totalIncome(null)
                .status("NORMAL")
                .build();
        when(incomeClient.getSummary(anyInt(), anyInt())).thenReturn(summary);

        assertThatThrownBy(() -> ruleEngineService.calculateCurrent(userId, "FREE"))
                .isInstanceOf(IncomeDataNotFoundException.class)
                .hasMessageContaining("Aucun revenu saisi");
    }

    @Test
    @DisplayName("calculateCurrent - throws IncomeDataNotFoundException when totalIncome is zero")
    void calculateCurrent_zeroIncome_throwsIncomeDataNotFound() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        MonthSummaryResponse summary = MonthSummaryResponse.builder()
                .totalIncome(BigDecimal.ZERO)
                .status("NORMAL")
                .build();
        when(incomeClient.getSummary(anyInt(), anyInt())).thenReturn(summary);

        assertThatThrownBy(() -> ruleEngineService.calculateCurrent(userId, "FREE"))
                .isInstanceOf(IncomeDataNotFoundException.class);
    }

    @Test
    @DisplayName("calculateCurrent - success with valid income data")
    void calculateCurrent_validData_success() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        MonthSummaryResponse summary = MonthSummaryResponse.builder()
                .totalIncome(new BigDecimal("500000"))
                .status("NORMAL")
                .build();
        when(incomeClient.getSummary(anyInt(), anyInt())).thenReturn(summary);

        AllocationResult result = ruleEngineService.calculateCurrent(userId, "FREE");

        assertThat(result).isNotNull();
        assertThat(result.getRule()).isEqualTo(RuleType.RULE_50_30_20);
    }

    @Test
    @DisplayName("getConfig - creates default config for new user")
    void getConfig_newUser_createsDefault() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(configRepository.save(any(UserRuleConfig.class))).thenReturn(defaultConfig);

        UserRuleConfigDto result = ruleEngineService.getConfig(userId);

        assertThat(result.getActiveRule()).isEqualTo(RuleType.RULE_50_30_20);
        verify(configRepository).save(any(UserRuleConfig.class));
    }

    @Test
    @DisplayName("getConfig - returns existing config")
    void getConfig_existingUser_returnsConfig() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        UserRuleConfigDto result = ruleEngineService.getConfig(userId);

        assertThat(result.getActiveRule()).isEqualTo(RuleType.RULE_50_30_20);
        assertThat(result.getJosephAbundanceSavingsPercent()).isEqualTo(30);
        verify(configRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateConfig - updates and returns new config")
    void updateConfig_success() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));

        UserRuleConfig updatedConfig = UserRuleConfig.builder()
                .id(defaultConfig.getId())
                .userId(userId)
                .activeRule(RuleType.RULE_JOSEPH)
                .josephAbundanceSavingsPercent(40)
                .josephLeanSavingsPercent(15)
                .build();
        when(configRepository.save(any(UserRuleConfig.class))).thenReturn(updatedConfig);

        UserRuleConfigRequest request = UserRuleConfigRequest.builder()
                .activeRule(RuleType.RULE_JOSEPH)
                .josephAbundanceSavingsPercent(40)
                .josephLeanSavingsPercent(15)
                .build();

        UserRuleConfigDto result = ruleEngineService.updateConfig(userId, "PREMIUM", request);

        assertThat(result.getActiveRule()).isEqualTo(RuleType.RULE_JOSEPH);
        assertThat(result.getJosephAbundanceSavingsPercent()).isEqualTo(40);
    }

    @Test
    @DisplayName("calculate - RULE_JOSEPH with incomeClient exception defaults to NORMAL status")
    void calculate_josephRuleClientFailure_defaultsNormal() {
        when(configRepository.findByUserId(userId)).thenReturn(Optional.of(defaultConfig));
        when(incomeClient.getSummary(anyInt(), anyInt())).thenThrow(new RuntimeException("timeout"));

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
}
