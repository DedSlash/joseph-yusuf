package com.josephyusuf.report.client;

import com.josephyusuf.report.client.dto.AuthUserDto;
import com.josephyusuf.report.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "auth-service", configuration = FeignConfig.class)
public interface AuthClient {

    @GetMapping("/api/auth/me")
    AuthUserDto me();
}
