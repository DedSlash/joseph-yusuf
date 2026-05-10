package com.josephyusuf.admin.dto;

import com.josephyusuf.admin.enums.Plan;
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
