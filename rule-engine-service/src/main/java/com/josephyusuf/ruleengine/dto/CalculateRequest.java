package com.josephyusuf.ruleengine.dto;

import com.josephyusuf.ruleengine.entity.RuleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateRequest {

    @NotNull(message = "La règle est obligatoire")
    private RuleType rule;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être positif")
    private BigDecimal totalIncome;

    private Integer month;
    private Integer year;
}
