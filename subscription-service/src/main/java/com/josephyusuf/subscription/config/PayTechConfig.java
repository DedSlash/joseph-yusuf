package com.josephyusuf.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "paytech")
@Getter
@Setter
public class PayTechConfig {

    private String apiKey;
    private String apiSecret;
    private String env;
    private String successUrl;
    private String cancelUrl;
    private String ipnUrl;

    public String getBaseUrl() {
        return "https://paytech.sn/api";
    }
}
