package com.josephyusuf.ruleengine.dto;

import com.josephyusuf.ruleengine.entity.RuleType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRuleConfigDto {

    private UUID id;
    private RuleType activeRule;
    private int josephAbundanceSavingsPercent;
    private int josephLeanSavingsPercent;
}
