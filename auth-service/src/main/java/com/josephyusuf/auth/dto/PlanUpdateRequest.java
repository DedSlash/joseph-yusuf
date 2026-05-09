package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Plan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanUpdateRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private Plan plan;
}
