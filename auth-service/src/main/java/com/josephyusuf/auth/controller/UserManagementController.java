package com.josephyusuf.auth.controller;

import com.josephyusuf.auth.dto.*;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService service;

    @PutMapping("/plan")
    public ResponseEntity<Void> updatePlanInternal(@Valid @RequestBody PlanUpdateRequest request) {
        service.updatePlanInternal(request.getUserId(), request.getPlan());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Plan plan,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.listUsers(page, size, plan, enabled, search));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getUser(id));
    }

    @PutMapping("/{id}/plan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updatePlan(@PathVariable UUID id,
                                              @Valid @RequestBody AdminPlanUpdateRequest request) {
        return ResponseEntity.ok(service.updatePlan(id, request.getPlan()));
    }

    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> setEnabled(@PathVariable UUID id,
                                              @Valid @RequestBody AdminBlockRequest request) {
        return ResponseEntity.ok(service.setEnabled(id, request.getEnabled()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateRole(@PathVariable UUID id,
                                              @Valid @RequestBody AdminRoleUpdateRequest request) {
        return ResponseEntity.ok(service.updateRole(id, request.getRole()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlanStatsResponse> planStats() {
        return ResponseEntity.ok(PlanStatsResponse.builder()
                .free(service.countByPlan(Plan.FREE))
                .premium(service.countByPlan(Plan.PREMIUM))
                .premiumPlus(service.countByPlan(Plan.PREMIUM_PLUS))
                .total(service.countAll())
                .admins(service.countByRole(Role.ADMIN))
                .activeUsers(service.countByEnabled(true))
                .blockedUsers(service.countByEnabled(false))
                .build());
    }
}
