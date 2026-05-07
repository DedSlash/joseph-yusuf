package com.josephyusuf.ruleengine.rule;

import com.josephyusuf.ruleengine.dto.AllocationResult;
import com.josephyusuf.ruleengine.entity.RuleType;

import java.math.BigDecimal;

public interface RuleCalculator {

    RuleType getSupportedRule();

    AllocationResult calculate(BigDecimal totalIncome, String monthStatus, int abundanceSavingsPercent, int leanSavingsPercent);
}
