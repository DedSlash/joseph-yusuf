package com.josephyusuf.subscription.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeValidation {

    private UUID id;
    private String code;
    private Integer discountPercent;
    private boolean valid;
    private String reason;
    private boolean lifetime;
    private String paddleDiscountId;
}
