package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Plan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistJoinRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Plan planTier;

    private String country;
    private String currency;
}
