package com.josephyusuf.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBlockRequest {

    @NotNull
    private Boolean enabled;
}
