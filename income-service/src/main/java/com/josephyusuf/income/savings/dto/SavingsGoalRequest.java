package com.josephyusuf.income.savings.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalRequest {

    @NotBlank(message = "Le nom de l'objectif est obligatoire")
    @Size(max = 150)
    private String name;

    @NotNull(message = "Le montant cible est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant cible doit être strictement positif")
    private BigDecimal targetAmount;

    @DecimalMin(value = "0.00", message = "L'objectif mensuel ne peut être négatif")
    private BigDecimal monthlyTarget;

    @DecimalMin(value = "0.00", message = "Le pourcentage doit être positif")
    @DecimalMax(value = "100.00", message = "Le pourcentage ne peut dépasser 100%")
    private BigDecimal monthlyTargetPercent;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate targetDate;
}
