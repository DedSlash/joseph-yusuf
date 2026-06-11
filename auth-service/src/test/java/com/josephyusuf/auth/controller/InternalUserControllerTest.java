package com.josephyusuf.auth.controller;

import com.josephyusuf.auth.dto.RenewalReminderEmailRequest;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.service.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InternalUserControllerTest {

    private static final String TOKEN = "internal-secret-token";

    @Mock private UserManagementService userManagementService;
    @Mock private HttpServletRequest httpRequest;

    private InternalUserController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalUserController(userManagementService);
        ReflectionTestUtils.setField(controller, "expectedToken", TOKEN);
    }

    private RenewalReminderEmailRequest sampleRequest() {
        return RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM)
                .type("J_MINUS_3")
                .expiresAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .couponApplied("EARLY50")
                .couponLifetime(true)
                .build();
    }

    @Test
    @DisplayName("renewal-reminder : token valide → 204 + délègue au service")
    void sendRenewalReminder_validToken_delegates() {
        when(httpRequest.getHeader("X-Internal-Token")).thenReturn(TOKEN);
        RenewalReminderEmailRequest req = sampleRequest();

        ResponseEntity<Void> response = controller.sendRenewalReminder(req, httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(userManagementService).sendRenewalReminderEmail(req);
    }

    @Test
    @DisplayName("renewal-reminder : token absent → 403, service non appelé")
    void sendRenewalReminder_missingToken_forbidden() {
        when(httpRequest.getHeader("X-Internal-Token")).thenReturn(null);

        ResponseEntity<Void> response = controller.sendRenewalReminder(sampleRequest(), httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        verify(userManagementService, never()).sendRenewalReminderEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("renewal-reminder : token incorrect → 403")
    void sendRenewalReminder_wrongToken_forbidden() {
        when(httpRequest.getHeader("X-Internal-Token")).thenReturn("bad-token");

        ResponseEntity<Void> response = controller.sendRenewalReminder(sampleRequest(), httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        verify(userManagementService, never()).sendRenewalReminderEmail(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("renewal-reminder : token attendu blank côté serveur → 403 (sécurité)")
    void sendRenewalReminder_blankExpectedToken_forbidden() {
        ReflectionTestUtils.setField(controller, "expectedToken", "");
        when(httpRequest.getHeader("X-Internal-Token")).thenReturn("anything");

        ResponseEntity<Void> response = controller.sendRenewalReminder(sampleRequest(), httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        verify(userManagementService, never()).sendRenewalReminderEmail(org.mockito.ArgumentMatchers.any());
    }
}
