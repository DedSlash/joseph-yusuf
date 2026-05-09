package com.josephyusuf.subscription.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeApplyRequest {

    private String code;
    private UUID userId;
    private String transactionId;
}
