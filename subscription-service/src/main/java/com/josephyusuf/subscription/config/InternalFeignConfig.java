package com.josephyusuf.subscription.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Intercepteur Feign pour les appels service-to-service authentifiés par
 * {@code X-Internal-Token}. À utiliser sur les clients qui appellent des
 * endpoints internes sans contexte JWT (cron, webhook).
 */
@Configuration
public class InternalFeignConfig {

    @Value("${app.internal.token:}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return template -> {
            if (internalToken != null && !internalToken.isBlank()) {
                template.header("X-Internal-Token", internalToken);
            }
        };
    }
}
