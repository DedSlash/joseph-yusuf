package com.josephyusuf.admin.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KpiOverviewResponse {

    private long totalUsers;
    private long activeUsers;
    private long blockedUsers;
    private long premiumUsers;
    private long premiumPlusUsers;
    private long freeUsers;
    private BigDecimal mrrEur;
    private BigDecimal mrrXof;
    private long activePromoCodes;
    private double conversionRate;
}
