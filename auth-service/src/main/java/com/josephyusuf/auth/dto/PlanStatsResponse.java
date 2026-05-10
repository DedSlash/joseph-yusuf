package com.josephyusuf.auth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanStatsResponse {

    private long free;
    private long premium;
    private long premiumPlus;
    private long total;
    private long admins;
    private long activeUsers;
    private long blockedUsers;
}
