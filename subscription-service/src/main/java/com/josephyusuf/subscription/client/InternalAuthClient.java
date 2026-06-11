package com.josephyusuf.subscription.client;

import com.josephyusuf.subscription.config.InternalFeignConfig;
import com.josephyusuf.subscription.dto.RenewalReminderEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour les endpoints internes auth-service authentifiés par
 * {@code X-Internal-Token}. Séparé de {@link AuthClient} qui relaye le JWT
 * utilisateur — ce client est utilisé par les crons (pas de JWT en contexte).
 */
@FeignClient(name = "auth-service", contextId = "internalAuthClient",
        configuration = InternalFeignConfig.class)
public interface InternalAuthClient {

    @PostMapping("/api/auth/users/internal/renewal-reminder")
    void sendRenewalReminderEmail(@RequestBody RenewalReminderEmailRequest request);
}
