package com.josephyusuf.income.controller;

import com.josephyusuf.income.dto.IncomeSourceDto;
import com.josephyusuf.income.dto.IncomeSourceRequest;
import com.josephyusuf.income.service.IncomeSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incomes/sources")
@RequiredArgsConstructor
public class IncomeSourceController {

    private final IncomeSourceService sourceService;

    @PostMapping
    public ResponseEntity<IncomeSourceDto> create(Authentication auth,
                                                   @Valid @RequestBody IncomeSourceRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sourceService.create(userId, plan, request));
    }

    @GetMapping
    public ResponseEntity<List<IncomeSourceDto>> list(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(sourceService.listByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeSourceDto> update(Authentication auth,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody IncomeSourceRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(sourceService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        sourceService.deactivate(userId, id);
        return ResponseEntity.noContent().build();
    }
}
