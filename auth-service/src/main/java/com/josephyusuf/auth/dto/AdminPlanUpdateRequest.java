package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Plan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPlanUpdateRequest {

    @NotNull
    private Plan plan;
}
