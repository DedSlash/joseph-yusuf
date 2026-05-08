package com.josephyusuf.alert.controller;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertDto>> list(Authentication auth,
                                               @RequestParam(required = false, defaultValue = "false") boolean unread) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(unread ? alertService.listUnread(userId) : alertService.listByUser(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(Map.of("count", alertService.countUnread(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<AlertDto> markAsRead(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(alertService.markAsRead(userId, id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        alertService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        alertService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
