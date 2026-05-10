package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRoleUpdateRequest {

    @NotNull
    private Role role;
}
