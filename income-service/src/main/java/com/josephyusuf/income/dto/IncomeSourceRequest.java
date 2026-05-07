package com.josephyusuf.income.dto;

import com.josephyusuf.income.entity.IncomeSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeSourceRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotNull(message = "Le type est obligatoire")
    private IncomeSourceType type;

    private String currency;
}
