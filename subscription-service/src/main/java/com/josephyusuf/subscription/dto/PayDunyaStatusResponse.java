package com.josephyusuf.subscription.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayDunyaStatusResponse {

    private String token;
    private String status;
    private Map<String, Object> customData;
}
