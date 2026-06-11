package com.josephyusuf.subscription.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "paddle")
public class PaddleConfig {
    private String apiKey;
    private String clientToken;
    private String webhookSecret;
    private boolean sandbox = true;
    private Prices prices = new Prices();

    public String getBaseUrl() {
        return sandbox
                ? "https://sandbox-api.paddle.com"
                : "https://api.paddle.com";
    }

    @Getter
    @Setter
    public static class Prices {
        private String premiumId;
        private String premiumPlusId;
        private String early50DiscountId;
    }
}
