package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PlanTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    @NotNull
    private PlanTier plan;

    @NotBlank
    private String paymentIntentId;
}
