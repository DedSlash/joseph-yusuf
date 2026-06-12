package com.josephyusuf.admin.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$",
            message = "code must be 3-50 chars, uppercase alphanumeric, underscore or dash")
    private String code;

    @Size(max = 255)
    private String description;

    @NotNull
    @Min(1)
    @Max(100)
    private Integer discountPercent;

    @Min(1)
    private Integer maxUses;

    private Instant expiresAt;

    @Builder.Default
    private boolean lifetime = false;

    @Pattern(regexp = "^dsc_[A-Za-z0-9]+$",
            message = "paddleDiscountId doit ressembler à dsc_xxx (vide autorisé)")
    @Size(max = 64)
    private String paddleDiscountId;
}
