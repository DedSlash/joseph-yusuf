package com.josephyusuf.admin.dto;

import com.josephyusuf.admin.enums.Role;
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
