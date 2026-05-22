package com.josephyusuf.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayTechPaymentResponse {

    private String refCommand;
    private String redirectUrl;
    private String mobileRedirectUrl;
}
