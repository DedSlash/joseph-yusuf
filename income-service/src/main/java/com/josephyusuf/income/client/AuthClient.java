package com.josephyusuf.income.client;

import com.josephyusuf.income.client.dto.UpdateProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PatchMapping("/api/auth/me")
    void updateProfile(@RequestBody UpdateProfileRequest request);
}
