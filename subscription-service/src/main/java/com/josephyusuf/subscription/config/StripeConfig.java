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

    @Value("${stripe.prices.premium-monthly-eur:price_premium_eur_placeholder}")
    private String premiumMonthlyEurPriceId;

    @Value("${stripe.prices.premium-monthly-xof:price_premium_xof_placeholder}")
    private String premiumMonthlyXofPriceId;

    @Value("${stripe.prices.premium-plus-monthly-eur:price_premium_plus_eur_placeholder}")
    private String premiumPlusMonthlyEurPriceId;

    @Value("${stripe.prices.premium-plus-monthly-xof:price_premium_plus_xof_placeholder}")
    private String premiumPlusMonthlyXofPriceId;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
}
