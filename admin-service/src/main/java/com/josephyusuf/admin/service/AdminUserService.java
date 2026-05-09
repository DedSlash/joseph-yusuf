package com.josephyusuf.admin.service;

import com.josephyusuf.admin.client.AuthClient;
import com.josephyusuf.admin.dto.*;
import com.josephyusuf.admin.enums.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AuthClient authClient;
    private final AuditLogService auditLogService;

    public PageResponse<UserDto> list(int page, int size, Plan plan, Boolean enabled, String search) {
        return authClient.listUsers(page, size, plan != null ? plan.name() : null, enabled, search);
    }

    public UserDto get(UUID id) {
        return authClient.getUser(id);
    }

    public UserDto updatePlan(UUID id, AdminPlanUpdateRequest request, UUID adminId, String ip) {
        UserDto updated = authClient.updatePlan(id, request);
        auditLogService.log(adminId, "USER_UPDATE_PLAN", "USER", id.toString(),
                "plan=" + request.getPlan(), ip);
        return updated;
    }

    public UserDto setEnabled(UUID id, AdminBlockRequest request, UUID adminId, String ip) {
        UserDto updated = authClient.setEnabled(id, request);
        auditLogService.log(adminId,
                Boolean.TRUE.equals(request.getEnabled()) ? "USER_UNBLOCK" : "USER_BLOCK",
                "USER", id.toString(), null, ip);
        return updated;
    }

    public UserDto updateRole(UUID id, AdminRoleUpdateRequest request, UUID adminId, String ip) {
        UserDto updated = authClient.updateRole(id, request);
        auditLogService.log(adminId, "USER_UPDATE_ROLE", "USER", id.toString(),
                "role=" + request.getRole(), ip);
        return updated;
    }

    public void delete(UUID id, UUID adminId, String ip) {
        authClient.deleteUser(id);
        auditLogService.log(adminId, "USER_DELETE", "USER", id.toString(), "RGPD", ip);
    }

    public PlanStatsResponse planStats() {
        return authClient.planStats();
    }
}
