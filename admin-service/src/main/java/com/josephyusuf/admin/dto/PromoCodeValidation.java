package com.josephyusuf.admin.dto;

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
}
