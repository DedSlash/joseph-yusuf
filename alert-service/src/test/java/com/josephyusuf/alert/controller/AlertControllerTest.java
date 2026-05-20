package com.josephyusuf.alert.controller;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.entity.AlertSeverity;
import com.josephyusuf.alert.entity.AlertType;
import com.josephyusuf.alert.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertController alertController;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ALERT_ID = UUID.randomUUID();

    private Authentication auth;
    private AlertDto alertDto;

    @BeforeEach
    void setUp() {
        auth = new UsernamePasswordAuthenticationToken(USER_ID.toString(), "PREMIUM", List.of());
        alertDto = AlertDto.builder()
                .id(ALERT_ID)
                .userId(USER_ID)
                .type(AlertType.INFO)
                .severity(AlertSeverity.INFO)
                .title("T")
                .message("M")
                .read(false)
                .build();
    }

    @Test
    void list_unreadFalse_returnsAllAlerts() {
        when(alertService.listByUser(USER_ID)).thenReturn(List.of(alertDto));

        ResponseEntity<List<AlertDto>> response = alertController.list(auth, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(alertDto);
        verify(alertService).listByUser(USER_ID);
    }

    @Test
    void list_unreadTrue_returnsOnlyUnread() {
        when(alertService.listUnread(USER_ID)).thenReturn(List.of(alertDto));

        ResponseEntity<List<AlertDto>> response = alertController.list(auth, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(alertService).listUnread(USER_ID);
        verify(alertService, never()).listByUser(any());
    }

    @Test
    void countUnread_returnsCountMap() {
        when(alertService.countUnread(USER_ID)).thenReturn(3L);

        ResponseEntity<Map<String, Long>> response = alertController.countUnread(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("count", 3L);
    }

    @Test
    void markAsRead_returnsUpdatedAlert() {
        when(alertService.markAsRead(USER_ID, ALERT_ID)).thenReturn(alertDto);

        ResponseEntity<AlertDto> response = alertController.markAsRead(auth, ALERT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(alertDto);
    }

    @Test
    void markAllAsRead_returnsNoContent() {
        ResponseEntity<Void> response = alertController.markAllAsRead(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(alertService).markAllAsRead(USER_ID);
    }

    @Test
    void delete_returnsNoContent() {
        ResponseEntity<Void> response = alertController.delete(auth, ALERT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(alertService).delete(USER_ID, ALERT_ID);
    }

    @Test
    void deleteAll_returnsNoContent() {
        ResponseEntity<Void> response = alertController.deleteAll(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(alertService).deleteAll(USER_ID);
    }
}
