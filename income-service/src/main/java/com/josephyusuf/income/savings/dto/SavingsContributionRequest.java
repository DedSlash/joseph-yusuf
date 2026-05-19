package com.josephyusuf.income.savings.dto;

import com.josephyusuf.income.savings.entity.SavingsContributionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsContributionRequest {

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être strictement positif")
    private BigDecimal amount;

    @Min(value = 1, message = "Le mois doit être entre 1 et 12")
    @Max(value = 12, message = "Le mois doit être entre 1 et 12")
    private Integer month;

    @Min(value = 2020, message = "L'année doit être >= 2020")
    private Integer year;

    private SavingsContributionType type;

    @Size(max = 255)
    private String note;
}
