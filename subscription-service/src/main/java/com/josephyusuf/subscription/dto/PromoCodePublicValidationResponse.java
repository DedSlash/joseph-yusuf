package com.josephyusuf.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodePublicValidationResponse {

    private boolean valid;
    private String code;
    private Integer discountPercent;
    private String description;
    private String reason;

    public static PromoCodePublicValidationResponse from(PromoCodeValidation validation) {
        if (validation.isValid()) {
            return PromoCodePublicValidationResponse.builder()
                    .valid(true)
                    .code(validation.getCode())
                    .discountPercent(validation.getDiscountPercent())
                    .build();
        }
        return PromoCodePublicValidationResponse.builder()
                .valid(false)
                .reason(validation.getReason())
                .build();
    }

    public static PromoCodePublicValidationResponse notFound() {
        return PromoCodePublicValidationResponse.builder()
                .valid(false)
                .reason("NOT_FOUND")
                .build();
    }
}
