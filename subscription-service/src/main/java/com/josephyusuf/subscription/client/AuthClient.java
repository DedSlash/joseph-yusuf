package com.josephyusuf.subscription.client;

import com.josephyusuf.subscription.dto.PlanUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @PutMapping("/api/auth/users/plan")
    void updatePlan(@RequestBody PlanUpdateRequest request);
}
