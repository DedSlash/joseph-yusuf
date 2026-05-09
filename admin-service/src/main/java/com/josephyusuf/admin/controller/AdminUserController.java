package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.dto.*;
import com.josephyusuf.admin.enums.Plan;
import com.josephyusuf.admin.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public ResponseEntity<PageResponse<UserDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Plan plan,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.list(page, size, plan, enabled, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}/plan")
    public ResponseEntity<UserDto> updatePlan(@PathVariable UUID id,
                                              @Valid @RequestBody AdminPlanUpdateRequest request,
                                              Authentication auth, HttpServletRequest http) {
        return ResponseEntity.ok(service.updatePlan(id, request, adminId(auth), clientIp(http)));
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<UserDto> setEnabled(@PathVariable UUID id,
                                              @Valid @RequestBody AdminBlockRequest request,
                                              Authentication auth, HttpServletRequest http) {
        return ResponseEntity.ok(service.setEnabled(id, request, adminId(auth), clientIp(http)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserDto> updateRole(@PathVariable UUID id,
                                              @Valid @RequestBody AdminRoleUpdateRequest request,
                                              Authentication auth, HttpServletRequest http) {
        return ResponseEntity.ok(service.updateRole(id, request, adminId(auth), clientIp(http)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       Authentication auth, HttpServletRequest http) {
        service.delete(id, adminId(auth), clientIp(http));
        return ResponseEntity.noContent().build();
    }

    private UUID adminId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
