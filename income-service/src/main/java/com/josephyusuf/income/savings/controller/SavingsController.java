package com.josephyusuf.income.savings.controller;

import com.josephyusuf.income.savings.dto.*;
import com.josephyusuf.income.savings.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @ExtractToSavingsService — sera servi par savings-service après extraction.
 */
@RestController
@RequestMapping("/api/incomes/savings")
@RequiredArgsConstructor
public class SavingsController {

    private final SavingsGoalService goalService;

    @PostMapping("/goals")
    public ResponseEntity<SavingsGoalDto> create(Authentication auth,
                                                 @Valid @RequestBody SavingsGoalRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.createGoal(userId, plan, request));
    }

    @GetMapping("/goals")
    public ResponseEntity<List<SavingsGoalDto>> list(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(goalService.getGoals(userId, plan));
    }

    @GetMapping("/goals/{id}")
    public ResponseEntity<SavingsGoalDto> getById(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(goalService.getGoalById(userId, id, plan));
    }

    @PutMapping("/goals/{id}")
    public ResponseEntity<SavingsGoalDto> update(Authentication auth,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody SavingsGoalRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(goalService.updateGoal(userId, id, request));
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        goalService.deleteGoal(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/goals/{id}/contributions")
    public ResponseEntity<SavingsContributionDto> addContribution(Authentication auth,
                                                                  @PathVariable UUID id,
                                                                  @Valid @RequestBody SavingsContributionRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(goalService.addContribution(userId, id, request));
    }

    @GetMapping("/goals/{id}/contributions")
    public ResponseEntity<Page<SavingsContributionDto>> listContributions(Authentication auth,
                                                                          @PathVariable UUID id,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(goalService.getContributions(userId, id, plan, page, size));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<SavingsRecommendationDto>> recommendations(Authentication auth,
                                                                          @RequestParam(required = false) Integer month,
                                                                          @RequestParam(required = false) Integer year) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return ResponseEntity.ok(goalService.getMonthlyRecommendation(userId, m, y));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<SavingsDashboardDto> dashboard(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(goalService.getDashboard(userId));
    }
}
