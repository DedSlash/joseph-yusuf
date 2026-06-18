package com.josephyusuf.report.service;

import com.josephyusuf.report.client.AuthClient;
import com.josephyusuf.report.client.IncomeClient;
import com.josephyusuf.report.client.RuleEngineClient;
import com.josephyusuf.report.client.dto.AuthUserDto;
import com.josephyusuf.report.dto.AllocationResultDto;
import com.josephyusuf.report.dto.AnnualMonthRow;
import com.josephyusuf.report.dto.AnnualReportData;
import com.josephyusuf.report.dto.MonthSummaryDto;
import com.josephyusuf.report.dto.MonthlyReportData;
import com.josephyusuf.report.dto.ReportResponse;
import com.josephyusuf.report.entity.ReportRecord;
import com.josephyusuf.report.entity.ReportType;
import com.josephyusuf.report.exception.PlanNotAllowedException;
import com.josephyusuf.report.exception.ReportNotFoundException;
import com.josephyusuf.report.exception.UnauthorizedAccessException;
import com.josephyusuf.report.mapper.ReportMapper;
import com.josephyusuf.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String PLAN_FREE = "FREE";

    private final IncomeClient incomeClient;
    private final RuleEngineClient ruleEngineClient;
    private final AuthClient authClient;
    private final JasperReportService jasperReportService;
    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;

    @Transactional
    public ReportResponse generateMonthly(UUID userId, String plan, int month, int year) {
        verifyPlan(plan);

        MonthSummaryDto summary = incomeClient.getSummary(month, year);
        AllocationResultDto allocation = ruleEngineClient.getResult(month, year);
        String currency = fetchDisplayCurrency();

        MonthlyReportData data = MonthlyReportData.builder()
                .userId(userId)
                .month(month)
                .year(year)
                .summary(summary)
                .allocation(allocation)
                .displayCurrency(currency)
                .build();

        byte[] pdf = jasperReportService.generateMonthlyPdf(data);
        String fileName = String.format("monthly-%d-%02d.pdf", year, month);

        ReportRecord saved = reportRepository.save(ReportRecord.builder()
                .userId(userId)
                .type(ReportType.MONTHLY)
                .month(month)
                .year(year)
                .fileName(fileName)
                .pdfContent(pdf)
                .sizeBytes(pdf.length)
                .build());

        log.info("Rapport mensuel généré userId={} {}/{} size={}B", userId, month, year, pdf.length);
        return reportMapper.toResponse(saved);
    }

    @Transactional
    public ReportResponse generateAnnual(UUID userId, String plan, int year) {
        verifyPlan(plan);

        List<AnnualMonthRow> rows = new ArrayList<>();
        BigDecimal annualTotal = BigDecimal.ZERO;
        int abundance = 0;
        int lean = 0;
        int normal = 0;

        for (int m = 1; m <= 12; m++) {
            MonthSummaryDto summary = incomeClient.getSummary(m, year);
            BigDecimal total = summary.getTotalIncome() != null ? summary.getTotalIncome() : BigDecimal.ZERO;
            annualTotal = annualTotal.add(total);
            String status = summary.getStatus();
            switch (status == null ? "" : status) {
                case "ABUNDANCE" -> abundance++;
                case "LEAN" -> lean++;
                case "NORMAL" -> normal++;
                default -> { /* unknown status - not counted */ }
            }
            rows.add(AnnualMonthRow.builder()
                    .month(m)
                    .totalIncome(total)
                    .status(status)
                    .build());
        }

        AnnualReportData data = AnnualReportData.builder()
                .userId(userId)
                .year(year)
                .totalAnnualIncome(annualTotal)
                .abundanceMonths(abundance)
                .leanMonths(lean)
                .normalMonths(normal)
                .rows(rows)
                .displayCurrency(fetchDisplayCurrency())
                .build();

        byte[] pdf = jasperReportService.generateAnnualPdf(data);
        String fileName = String.format("annual-%d.pdf", year);

        ReportRecord saved = reportRepository.save(ReportRecord.builder()
                .userId(userId)
                .type(ReportType.ANNUAL)
                .year(year)
                .fileName(fileName)
                .pdfContent(pdf)
                .sizeBytes(pdf.length)
                .build());

        log.info("Rapport annuel généré userId={} year={} size={}B", userId, year, pdf.length);
        return reportMapper.toResponse(saved);
    }

    public Page<ReportResponse> list(UUID userId, Pageable pageable) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(reportMapper::toResponse);
    }

    public ReportRecord download(UUID userId, UUID reportId) {
        ReportRecord record = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Rapport introuvable : " + reportId));
        if (!record.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Accès refusé au rapport " + reportId);
        }
        return record;
    }

    private void verifyPlan(String plan) {
        if (plan == null || PLAN_FREE.equalsIgnoreCase(plan)) {
            throw new PlanNotAllowedException("Les rapports PDF sont réservés aux plans PREMIUM et PREMIUM_PLUS");
        }
    }

    private String fetchDisplayCurrency() {
        try {
            AuthUserDto user = authClient.me();
            return user != null && user.getCurrency() != null ? user.getCurrency() : "XOF";
        } catch (Exception e) {
            log.warn("Failed to fetch user currency, fallback to XOF: {}", e.getMessage());
            return "XOF";
        }
    }
}
