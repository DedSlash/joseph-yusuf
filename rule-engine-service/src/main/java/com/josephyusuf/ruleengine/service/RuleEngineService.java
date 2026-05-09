package com.josephyusuf.ruleengine.service;

import com.josephyusuf.ruleengine.client.IncomeClient;
import com.josephyusuf.ruleengine.dto.*;
import com.josephyusuf.ruleengine.entity.RuleType;
import com.josephyusuf.ruleengine.entity.UserRuleConfig;
import com.josephyusuf.ruleengine.exception.IncomeDataNotFoundException;
import com.josephyusuf.ruleengine.exception.RuleNotAccessibleException;
import com.josephyusuf.ruleengine.producer.RuleEventProducer;
import com.josephyusuf.ruleengine.repository.UserRuleConfigRepository;
import com.josephyusuf.ruleengine.rule.RuleCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RuleEngineService {

    private final Map<RuleType, RuleCalculator> calculators;
    private final UserRuleConfigRepository configRepository;
    private final IncomeClient incomeClient;
    private final RuleEventProducer ruleEventProducer;

    public RuleEngineService(List<RuleCalculator> calculatorList,
                             UserRuleConfigRepository configRepository,
                             IncomeClient incomeClient,
                             RuleEventProducer ruleEventProducer) {
        this.configRepository = configRepository;
        this.incomeClient = incomeClient;
        this.ruleEventProducer = ruleEventProducer;
        this.calculators = calculatorList.stream()
                .collect(Collectors.toMap(RuleCalculator::getSupportedRule, Function.identity()));
    }

    public AllocationResult calculate(UUID userId, String plan, CalculateRequest request) {
        checkPlanAccess(plan, request.getRule());

        UserRuleConfig config = getOrCreateConfig(userId);
        String monthStatus = null;

        if (request.getRule() == RuleType.RULE_JOSEPH && request.getMonth() != null && request.getYear() != null) {
            try {
                MonthSummaryResponse summary = incomeClient.getSummary(request.getMonth(), request.getYear());
                monthStatus = summary.getStatus();
            } catch (Exception e) {
                monthStatus = "NORMAL";
            }
        }

        RuleCalculator calculator = calculators.get(request.getRule());
        AllocationResult result = calculator.calculate(
                request.getTotalIncome(),
                monthStatus,
                config.getJosephAbundanceSavingsPercent(),
                config.getJosephLeanSavingsPercent()
        );

        ruleEventProducer.publishRuleApplied(userId, result, request.getMonth(), request.getYear());
        return result;
    }

    public AllocationResult calculateCurrent(UUID userId, String plan) {
        UserRuleConfig config = getOrCreateConfig(userId);
        checkPlanAccess(plan, config.getActiveRule());

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        MonthSummaryResponse summary;
        try {
            summary = incomeClient.getSummary(month, year);
        } catch (Exception e) {
            throw new IncomeDataNotFoundException(
                    "Pas de données de revenus pour le mois courant (" + month + "/" + year + ")");
        }

        if (summary.getTotalIncome() == null || summary.getTotalIncome().compareTo(BigDecimal.ZERO) == 0) {
            throw new IncomeDataNotFoundException(
                    "Aucun revenu saisi pour " + month + "/" + year);
        }

        RuleCalculator calculator = calculators.get(config.getActiveRule());
        AllocationResult result = calculator.calculate(
                summary.getTotalIncome(),
                summary.getStatus(),
                config.getJosephAbundanceSavingsPercent(),
                config.getJosephLeanSavingsPercent()
        );

        ruleEventProducer.publishRuleApplied(userId, result, month, year);
        return result;
    }

    public UserRuleConfigDto getConfig(UUID userId) {
        UserRuleConfig config = getOrCreateConfig(userId);
        return toDto(config);
    }

    @Transactional
    public UserRuleConfigDto updateConfig(UUID userId, String plan, UserRuleConfigRequest request) {
        checkPlanAccess(plan, request.getActiveRule());

        UserRuleConfig config = getOrCreateConfig(userId);
        config.setActiveRule(request.getActiveRule());
        config.setJosephAbundanceSavingsPercent(request.getJosephAbundanceSavingsPercent());
        config.setJosephLeanSavingsPercent(request.getJosephLeanSavingsPercent());
        config = configRepository.save(config);
        return toDto(config);
    }

    public List<RuleAvailability> getAvailableRules(String plan) {
        boolean isPremium = "PREMIUM".equals(plan) || "PREMIUM_PLUS".equals(plan);

        return List.of(
                RuleAvailability.builder()
                        .rule(RuleType.RULE_50_30_20)
                        .name("Règle 50/30/20")
                        .locked(false)
                        .build(),
                RuleAvailability.builder()
                        .rule(RuleType.RULE_80_20)
                        .name("Règle 80/20 (Pareto)")
                        .locked(!isPremium)
                        .build(),
                RuleAvailability.builder()
                        .rule(RuleType.RULE_70_20_10)
                        .name("Règle 70/20/10")
                        .locked(!isPremium)
                        .build(),
                RuleAvailability.builder()
                        .rule(RuleType.RULE_JOSEPH)
                        .name("Principe de Joseph")
                        .locked(!isPremium)
                        .build()
        );
    }

    private void checkPlanAccess(String plan, RuleType rule) {
        if ("FREE".equals(plan) && rule != RuleType.RULE_50_30_20) {
            throw new RuleNotAccessibleException(
                    "Règle disponible en Premium uniquement. Passez en Premium.");
        }
    }

    private UserRuleConfig getOrCreateConfig(UUID userId) {
        return configRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserRuleConfig config = UserRuleConfig.builder()
                            .userId(userId)
                            .activeRule(RuleType.RULE_50_30_20)
                            .josephAbundanceSavingsPercent(30)
                            .josephLeanSavingsPercent(10)
                            .build();
                    return configRepository.save(config);
                });
    }

    private UserRuleConfigDto toDto(UserRuleConfig config) {
        return UserRuleConfigDto.builder()
                .id(config.getId())
                .activeRule(config.getActiveRule())
                .josephAbundanceSavingsPercent(config.getJosephAbundanceSavingsPercent())
                .josephLeanSavingsPercent(config.getJosephLeanSavingsPercent())
                .build();
    }
}
