package com.josephyusuf.report.service;

import com.josephyusuf.report.client.IncomeClient;
import com.josephyusuf.report.client.RuleEngineClient;
import com.josephyusuf.report.dto.AllocationResultDto;
import com.josephyusuf.report.dto.MonthSummaryDto;
import com.josephyusuf.report.dto.ReportResponse;
import com.josephyusuf.report.entity.ReportRecord;
import com.josephyusuf.report.entity.ReportType;
import com.josephyusuf.report.exception.PlanNotAllowedException;
import com.josephyusuf.report.exception.ReportNotFoundException;
import com.josephyusuf.report.exception.UnauthorizedAccessException;
import com.josephyusuf.report.mapper.ReportMapper;
import com.josephyusuf.report.repository.ReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private IncomeClient incomeClient;
    @Mock private RuleEngineClient ruleEngineClient;
    @Mock private JasperReportService jasperReportService;
    @Mock private ReportRepository reportRepository;
    @Mock private ReportMapper reportMapper;

    @InjectMocks
    private ReportService reportService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID REPORT_ID = UUID.randomUUID();
    private static final byte[] FAKE_PDF = new byte[]{1, 2, 3, 4};

    private MonthSummaryDto sampleSummary(int month, String status, BigDecimal total) {
        return MonthSummaryDto.builder()
                .userId(USER_ID).month(month).year(2026)
                .totalIncome(total).averageLast3Months(new BigDecimal("400000"))
                .status(status).percentageVsAverage(new BigDecimal("125.00"))
                .build();
    }

    private AllocationResultDto sampleAllocation() {
        return AllocationResultDto.builder()
                .rule("RULE_JOSEPH").totalIncome(new BigDecimal("500000"))
                .monthStatus("ABUNDANCE").message("Mois d'abondance").allocations(List.of())
                .build();
    }

    @Nested
    @DisplayName("generateMonthly")
    class GenerateMonthlyTests {

        @Test
        @DisplayName("PREMIUM : génération réussie")
        void generateMonthly_premium_success() {
            when(incomeClient.getSummary(5, 2026)).thenReturn(sampleSummary(5, "ABUNDANCE", new BigDecimal("500000")));
            when(ruleEngineClient.getResult(5, 2026)).thenReturn(sampleAllocation());
            when(jasperReportService.generateMonthlyPdf(any())).thenReturn(FAKE_PDF);

            ReportRecord saved = ReportRecord.builder()
                    .id(REPORT_ID).userId(USER_ID).type(ReportType.MONTHLY)
                    .month(5).year(2026).fileName("monthly-2026-05.pdf")
                    .pdfContent(FAKE_PDF).sizeBytes(FAKE_PDF.length).build();
            when(reportRepository.save(any(ReportRecord.class))).thenReturn(saved);
            when(reportMapper.toResponse(saved)).thenReturn(
                    ReportResponse.builder().id(REPORT_ID).type(ReportType.MONTHLY).month(5).year(2026).build());

            ReportResponse result = reportService.generateMonthly(USER_ID, "PREMIUM", 5, 2026);

            assertThat(result.getId()).isEqualTo(REPORT_ID);
            assertThat(result.getType()).isEqualTo(ReportType.MONTHLY);
            verify(jasperReportService).generateMonthlyPdf(any());
            verify(reportRepository).save(any(ReportRecord.class));
        }

        @Test
        @DisplayName("FREE : PlanNotAllowedException")
        void generateMonthly_free_throws() {
            assertThatThrownBy(() -> reportService.generateMonthly(USER_ID, "FREE", 5, 2026))
                    .isInstanceOf(PlanNotAllowedException.class);
            verifyNoInteractions(incomeClient, ruleEngineClient, jasperReportService, reportRepository);
        }

        @Test
        @DisplayName("plan null : PlanNotAllowedException")
        void generateMonthly_nullPlan_throws() {
            assertThatThrownBy(() -> reportService.generateMonthly(USER_ID, null, 5, 2026))
                    .isInstanceOf(PlanNotAllowedException.class);
        }
    }

    @Nested
    @DisplayName("generateAnnual")
    class GenerateAnnualTests {

        @Test
        @DisplayName("PREMIUM_PLUS : agrège les 12 mois")
        void generateAnnual_aggregatesAllMonths() {
            for (int m = 1; m <= 12; m++) {
                String status = m <= 4 ? "ABUNDANCE" : (m <= 8 ? "LEAN" : "NORMAL");
                BigDecimal amount = new BigDecimal("100000");
                when(incomeClient.getSummary(m, 2026)).thenReturn(sampleSummary(m, status, amount));
            }
            when(jasperReportService.generateAnnualPdf(any())).thenReturn(FAKE_PDF);

            ReportRecord saved = ReportRecord.builder()
                    .id(REPORT_ID).userId(USER_ID).type(ReportType.ANNUAL)
                    .year(2026).fileName("annual-2026.pdf")
                    .pdfContent(FAKE_PDF).sizeBytes(FAKE_PDF.length).build();
            when(reportRepository.save(any(ReportRecord.class))).thenReturn(saved);
            when(reportMapper.toResponse(saved)).thenReturn(
                    ReportResponse.builder().id(REPORT_ID).type(ReportType.ANNUAL).year(2026).build());

            ReportResponse result = reportService.generateAnnual(USER_ID, "PREMIUM_PLUS", 2026);

            assertThat(result.getYear()).isEqualTo(2026);
            verify(incomeClient, times(12)).getSummary(anyInt(), eq(2026));
            verify(jasperReportService).generateAnnualPdf(any());
        }

        @Test
        @DisplayName("FREE : PlanNotAllowedException")
        void generateAnnual_free_throws() {
            assertThatThrownBy(() -> reportService.generateAnnual(USER_ID, "FREE", 2026))
                    .isInstanceOf(PlanNotAllowedException.class);
        }

        @Test
        @DisplayName("totalIncome null toléré : ne casse pas l'agrégation")
        void generateAnnual_nullTotal_doesNotFail() {
            for (int m = 1; m <= 12; m++) {
                MonthSummaryDto s = MonthSummaryDto.builder()
                        .userId(USER_ID).month(m).year(2026)
                        .totalIncome(null).status(null).build();
                when(incomeClient.getSummary(m, 2026)).thenReturn(s);
            }
            when(jasperReportService.generateAnnualPdf(any())).thenReturn(FAKE_PDF);
            when(reportRepository.save(any(ReportRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reportMapper.toResponse(any())).thenReturn(ReportResponse.builder().year(2026).build());

            ReportResponse result = reportService.generateAnnual(USER_ID, "PREMIUM", 2026);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("Retourne la page mappée")
        void list_returnsMappedPage() {
            ReportRecord r = ReportRecord.builder().id(REPORT_ID).userId(USER_ID).type(ReportType.MONTHLY).build();
            Page<ReportRecord> page = new PageImpl<>(List.of(r));
            when(reportRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(page);
            when(reportMapper.toResponse(r)).thenReturn(ReportResponse.builder().id(REPORT_ID).build());

            Page<ReportResponse> result = reportService.list(USER_ID, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(REPORT_ID);
        }
    }

    @Nested
    @DisplayName("download")
    class DownloadTests {

        @Test
        @DisplayName("Téléchargement réussi")
        void download_success() {
            ReportRecord record = ReportRecord.builder()
                    .id(REPORT_ID).userId(USER_ID).pdfContent(FAKE_PDF).fileName("monthly.pdf").build();
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(record));

            ReportRecord result = reportService.download(USER_ID, REPORT_ID);

            assertThat(result.getPdfContent()).isEqualTo(FAKE_PDF);
        }

        @Test
        @DisplayName("ReportNotFoundException")
        void download_notFound() {
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.download(USER_ID, REPORT_ID))
                    .isInstanceOf(ReportNotFoundException.class);
        }

        @Test
        @DisplayName("Mauvais propriétaire : UnauthorizedAccessException")
        void download_wrongOwner() {
            ReportRecord record = ReportRecord.builder()
                    .id(REPORT_ID).userId(UUID.randomUUID()).build();
            when(reportRepository.findById(REPORT_ID)).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> reportService.download(USER_ID, REPORT_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }
}
