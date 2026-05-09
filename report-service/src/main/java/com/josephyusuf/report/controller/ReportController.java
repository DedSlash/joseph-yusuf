package com.josephyusuf.report.controller;

import com.josephyusuf.report.dto.ReportRequest;
import com.josephyusuf.report.dto.ReportResponse;
import com.josephyusuf.report.entity.ReportRecord;
import com.josephyusuf.report.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/monthly")
    public ResponseEntity<ReportResponse> generateMonthly(Authentication auth,
                                                          @Valid @RequestBody MonthlyReportRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(reportService.generateMonthly(userId, plan, request.getMonth(), request.getYear()));
    }

    @PostMapping("/annual")
    public ResponseEntity<ReportResponse> generateAnnual(Authentication auth,
                                                         @Valid @RequestBody ReportRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(reportService.generateAnnual(userId, plan, request.getYear()));
    }

    @GetMapping
    public ResponseEntity<Page<ReportResponse>> list(Authentication auth, Pageable pageable) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(reportService.list(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        ReportRecord record = reportService.download(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(record.getPdfContent());
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MonthlyReportRequest {
        @NotNull
        @jakarta.validation.constraints.Min(1)
        @jakarta.validation.constraints.Max(12)
        private Integer month;

        @NotNull
        @jakarta.validation.constraints.Min(2020)
        private Integer year;
    }
}
