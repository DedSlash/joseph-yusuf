package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PlanTier;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanUpdateRequest {

    private UUID userId;
    private PlanTier plan;
}
