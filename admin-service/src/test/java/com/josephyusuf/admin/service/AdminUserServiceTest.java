package com.josephyusuf.admin.service;

import com.josephyusuf.admin.client.AuthClient;
import com.josephyusuf.admin.dto.*;
import com.josephyusuf.admin.enums.Plan;
import com.josephyusuf.admin.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AuthClient authClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminUserService service;

    @Test
    @DisplayName("updatePlan - delegates to authClient and logs audit")
    void updatePlan_audited() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AdminPlanUpdateRequest request = AdminPlanUpdateRequest.builder().plan(Plan.PREMIUM).build();
        when(authClient.updatePlan(eq(userId), any())).thenReturn(UserDto.builder().id(userId).build());

        service.updatePlan(userId, request, adminId, "127.0.0.1");

        verify(authClient).updatePlan(userId, request);
        verify(auditLogService).log(eq(adminId), eq("USER_UPDATE_PLAN"), eq("USER"),
                eq(userId.toString()), any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("updateRole - delegates and logs")
    void updateRole_audited() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AdminRoleUpdateRequest request = AdminRoleUpdateRequest.builder().role(Role.ADMIN).build();
        when(authClient.updateRole(eq(userId), any())).thenReturn(UserDto.builder().id(userId).build());

        service.updateRole(userId, request, adminId, "127.0.0.1");

        verify(authClient).updateRole(userId, request);
        verify(auditLogService).log(eq(adminId), eq("USER_UPDATE_ROLE"), eq("USER"),
                eq(userId.toString()), any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("delete - delegates and logs")
    void delete_audited() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        service.delete(userId, adminId, "127.0.0.1");

        verify(authClient).deleteUser(userId);
        verify(auditLogService).log(eq(adminId), eq("USER_DELETE"), eq("USER"),
                eq(userId.toString()), eq("RGPD"), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("list - delegates with plan name")
    void list_withPlan() {
        PageResponse<UserDto> page = PageResponse.<UserDto>builder().build();
        when(authClient.listUsers(0, 20, "PREMIUM", true, "joe")).thenReturn(page);

        assertThat(service.list(0, 20, Plan.PREMIUM, true, "joe")).isSameAs(page);
        verify(authClient).listUsers(0, 20, "PREMIUM", true, "joe");
    }

    @Test
    @DisplayName("list - null plan is forwarded as null")
    void list_withoutPlan() {
        PageResponse<UserDto> page = PageResponse.<UserDto>builder().build();
        when(authClient.listUsers(0, 20, null, null, null)).thenReturn(page);

        assertThat(service.list(0, 20, null, null, null)).isSameAs(page);
    }

    @Test
    @DisplayName("get - delegates")
    void get_delegates() {
        UUID userId = UUID.randomUUID();
        UserDto user = UserDto.builder().id(userId).build();
        when(authClient.getUser(userId)).thenReturn(user);

        assertThat(service.get(userId)).isSameAs(user);
    }

    @Test
    @DisplayName("setEnabled - block writes USER_BLOCK audit")
    void setEnabled_block() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AdminBlockRequest request = AdminBlockRequest.builder().enabled(false).build();
        when(authClient.setEnabled(eq(userId), any())).thenReturn(UserDto.builder().id(userId).build());

        service.setEnabled(userId, request, adminId, "127.0.0.1");

        verify(auditLogService).log(eq(adminId), eq("USER_BLOCK"), eq("USER"),
                eq(userId.toString()), isNull(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("setEnabled - unblock writes USER_UNBLOCK audit")
    void setEnabled_unblock() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AdminBlockRequest request = AdminBlockRequest.builder().enabled(true).build();
        when(authClient.setEnabled(eq(userId), any())).thenReturn(UserDto.builder().id(userId).build());

        service.setEnabled(userId, request, adminId, "127.0.0.1");

        verify(auditLogService).log(eq(adminId), eq("USER_UNBLOCK"), eq("USER"),
                eq(userId.toString()), isNull(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("planStats - delegates to authClient")
    void planStats_delegates() {
        PlanStatsResponse stats = PlanStatsResponse.builder().build();
        when(authClient.planStats()).thenReturn(stats);

        assertThat(service.planStats()).isSameAs(stats);
    }
}
