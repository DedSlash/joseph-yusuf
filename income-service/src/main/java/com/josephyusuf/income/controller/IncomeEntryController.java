package com.josephyusuf.income.controller;

import com.josephyusuf.income.dto.IncomeEntryDto;
import com.josephyusuf.income.dto.IncomeEntryRequest;
import com.josephyusuf.income.service.IncomeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incomes/entries")
@RequiredArgsConstructor
public class IncomeEntryController {

    private final IncomeEntryService entryService;

    @PostMapping
    public ResponseEntity<IncomeEntryDto> create(Authentication auth,
                                                  @Valid @RequestBody IncomeEntryRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entryService.create(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<IncomeEntryDto>> list(Authentication auth,
                                                      @RequestParam int month,
                                                      @RequestParam int year) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(entryService.listByMonthYear(userId, month, year));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeEntryDto> update(Authentication auth,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody IncomeEntryRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(entryService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        entryService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
