package com.josephyusuf.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "paydunya")
@Getter
@Setter
public class PayDunyaConfig {

    private String masterKey;
    private String privateKey;
    private String token;
    private String mode;
    private String callbackUrl;
    private String returnUrl;
    private String cancelUrl;

    public String getBaseUrl() {
        return "test".equals(mode)
                ? "https://app.paydunya.com/sandbox-api/v1"
                : "https://app.paydunya.com/api/v1";
    }
}
