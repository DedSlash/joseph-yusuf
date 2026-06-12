package com.josephyusuf.admin.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeResponse {

    private UUID id;
    private String code;
    private String description;
    private Integer discountPercent;
    private Integer maxUses;
    private Integer usedCount;
    private Instant expiresAt;
    private boolean active;
    private boolean lifetime;
    private String paddleDiscountId;
    private Instant createdAt;
}
