package com.josephyusuf.subscription.dto;

import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProviderResponse {

    private PaymentProvider provider;
    private String transactionId;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String redirectUrl;
    private String message;
}
