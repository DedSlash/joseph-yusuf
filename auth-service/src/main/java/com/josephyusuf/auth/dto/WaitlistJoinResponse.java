package com.josephyusuf.auth.dto;

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
public class WaitlistJoinResponse {

    private String email;
    private String planTier;
    private String promoCodeReserved;
    private boolean alreadyRegistered;
    private String message;
}
