package com.josephyusuf.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddResponseRequest {

    @NotBlank
    private String message;
}
