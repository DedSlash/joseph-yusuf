package com.josephyusuf.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaddleRequest {

    @NotNull(message = "Le plan est obligatoire")
    private String planTier;

    private String couponCode;
}
