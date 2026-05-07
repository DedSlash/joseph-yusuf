package com.josephyusuf.ruleengine.dto;

import com.josephyusuf.ruleengine.entity.RuleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRuleConfigRequest {

    @NotNull(message = "La règle active est obligatoire")
    private RuleType activeRule;

    @Min(value = 20, message = "Le pourcentage d'épargne abondance doit être entre 20 et 50")
    @Max(value = 50, message = "Le pourcentage d'épargne abondance doit être entre 20 et 50")
    private int josephAbundanceSavingsPercent;

    @Min(value = 5, message = "Le pourcentage d'épargne disette doit être entre 5 et 20")
    @Max(value = 20, message = "Le pourcentage d'épargne disette doit être entre 5 et 20")
    private int josephLeanSavingsPercent;
}
