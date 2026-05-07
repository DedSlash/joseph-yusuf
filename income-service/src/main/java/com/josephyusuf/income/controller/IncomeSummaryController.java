package com.josephyusuf.income.controller;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.service.MonthSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeSummaryController {

    private final MonthSummaryService summaryService;

    @GetMapping("/summary")
    public ResponseEntity<MonthSummary> getSummary(Authentication auth,
                                                    @RequestParam int month,
                                                    @RequestParam int year) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(summaryService.getSummary(userId, month, year));
    }

    @GetMapping("/history")
    public ResponseEntity<List<MonthSummary>> getHistory(Authentication auth,
                                                          @RequestParam(defaultValue = "12") int months) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(summaryService.getHistory(userId, months));
    }
}
