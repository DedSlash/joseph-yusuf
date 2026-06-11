package com.josephyusuf.subscription.client;

import com.josephyusuf.subscription.config.InternalFeignConfig;
import com.josephyusuf.subscription.dto.InternalAlertRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "alert-service", configuration = InternalFeignConfig.class)
public interface AlertClient {

    @PostMapping("/api/alerts/internal")
    void createInternalAlert(@RequestBody InternalAlertRequest request);
}
