package com.josephyusuf.auth.client;

import com.josephyusuf.auth.client.dto.InternalAlertRequest;
import com.josephyusuf.auth.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "alert-service", configuration = InternalFeignConfig.class)
public interface AlertClient {

    @PostMapping("/api/alerts/internal")
    void createInternalAlert(@RequestBody InternalAlertRequest request);
}
