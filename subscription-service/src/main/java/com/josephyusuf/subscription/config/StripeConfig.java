package com.josephyusuf.subscription.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${stripe.prices.premium-eur:499}")
    private long premiumPriceEur;

    @Value("${stripe.prices.premium-plus-eur:999}")
    private long premiumPlusPriceEur;

    @Value("${stripe.prices.premium-xof:3000}")
    private long premiumPriceXof;

    @Value("${stripe.prices.premium-plus-xof:6000}")
    private long premiumPlusPriceXof;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
}
