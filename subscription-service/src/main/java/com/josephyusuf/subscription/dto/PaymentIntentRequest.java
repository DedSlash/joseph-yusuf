package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PlanTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentRequest {

    @NotNull
    private PlanTier plan;

    @NotBlank
    @Pattern(regexp = "EUR|XOF", message = "currency must be EUR or XOF")
    private String currency;
}
