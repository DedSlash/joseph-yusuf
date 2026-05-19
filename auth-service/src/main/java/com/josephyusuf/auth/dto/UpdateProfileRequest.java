package com.josephyusuf.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    private String firstName;

    private String lastName;

    @Size(min = 2, max = 10, message = "Code pays invalide")
    private String country;

    @Size(min = 3, max = 10, message = "Code devise invalide")
    private String currency;
}
