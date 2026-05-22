package com.josephyusuf.subscription.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayDunyaInvoiceResponse {

    private String token;
    private String invoiceUrl;
}
