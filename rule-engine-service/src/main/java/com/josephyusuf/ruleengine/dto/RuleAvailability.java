package com.josephyusuf.ruleengine.dto;

import com.josephyusuf.ruleengine.entity.RuleType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleAvailability {

    private RuleType rule;
    private String name;
    private boolean locked;
}
