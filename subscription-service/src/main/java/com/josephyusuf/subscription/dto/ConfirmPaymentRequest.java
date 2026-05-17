package com.josephyusuf.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentRequest {

    @NotBlank
    private String paymentIntentId;
}
