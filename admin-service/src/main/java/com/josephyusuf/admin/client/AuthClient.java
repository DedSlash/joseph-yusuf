package com.josephyusuf.admin.client;

import com.josephyusuf.admin.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/auth/users")
    PageResponse<UserDto> listUsers(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String plan,
                                    @RequestParam(required = false) Boolean enabled,
                                    @RequestParam(required = false) String search);

    @GetMapping("/api/auth/users/{id}")
    UserDto getUser(@PathVariable("id") UUID id);

    @PutMapping("/api/auth/users/{id}/plan")
    UserDto updatePlan(@PathVariable("id") UUID id, @RequestBody AdminPlanUpdateRequest request);

    @PutMapping("/api/auth/users/{id}/block")
    UserDto setEnabled(@PathVariable("id") UUID id, @RequestBody AdminBlockRequest request);

    @PutMapping("/api/auth/users/{id}/role")
    UserDto updateRole(@PathVariable("id") UUID id, @RequestBody AdminRoleUpdateRequest request);

    @DeleteMapping("/api/auth/users/{id}")
    void deleteUser(@PathVariable("id") UUID id);

    @GetMapping("/api/auth/users/stats/plans")
    PlanStatsResponse planStats();

    @GetMapping("/api/auth/admin/payments-toggle/status")
    PaymentsToggleStatusDto paymentsToggleStatus();

    @PostMapping("/api/auth/admin/payments-toggle/activate")
    PaymentsToggleActivateResponse paymentsToggleActivate();

    @PostMapping("/api/auth/admin/payments-toggle/deactivate")
    PaymentsToggleDeactivateResponse paymentsToggleDeactivate();

    @PostMapping("/api/auth/admin/payments-toggle/preview-email/{template}")
    Map<String, Object> paymentsTogglePreviewEmail(@PathVariable("template") String template,
                                                    @RequestParam("to") String to,
                                                    @RequestParam(value = "firstName", required = false) String firstName);
}
