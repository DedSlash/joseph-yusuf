package com.josephyusuf.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeStatsResponse {

    private UUID id;
    private String code;
    private long totalUsages;
    private Integer maxUses;
    private BigDecimal estimatedSavings;
    private boolean active;
}
