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
public class WavePaymentRequest {

    @NotNull
    private PlanTier plan;

    @NotBlank
    @Pattern(regexp = "\\+?[0-9]{8,15}", message = "phone must be a valid international number")
    private String phoneNumber;
}
