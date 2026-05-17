package com.josephyusuf.subscription.client;

import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.dto.PromoCodeValidation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "admin-service")
public interface AdminClient {

    @GetMapping("/api/admin/promo-codes/validate")
    PromoCodeValidation validate(@RequestParam("code") String code,
                                 @RequestParam("userId") UUID userId);

    @PostMapping("/api/internal/promo-codes/apply")
    PromoCodeValidation apply(@RequestBody PromoCodeApplyRequest request);
}
