package com.josephyusuf.report.service;

import com.josephyusuf.report.dto.AllocationLineDto;
import com.josephyusuf.report.dto.AllocationResultDto;
import com.josephyusuf.report.dto.AnnualMonthRow;
import com.josephyusuf.report.dto.AnnualReportData;
import com.josephyusuf.report.dto.MonthSummaryDto;
import com.josephyusuf.report.dto.MonthlyReportData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JasperReportServiceTest {

    private final JasperReportService service = new JasperReportService();

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("generateMonthlyPdf produit un PDF non vide commençant par %PDF")
    void generateMonthlyPdf_producesPdfBytes() {
        MonthlyReportData data = MonthlyReportData.builder()
                .userId(USER_ID)
                .month(5)
                .year(2026)
                .summary(MonthSummaryDto.builder()
                        .userId(USER_ID).month(5).year(2026)
                        .totalIncome(new BigDecimal("500000"))
                        .averageLast3Months(new BigDecimal("400000"))
                        .status("ABUNDANCE")
                        .percentageVsAverage(new BigDecimal("125.00"))
                        .build())
                .allocation(AllocationResultDto.builder()
                        .rule("RULE_50_30_20")
                        .totalIncome(new BigDecimal("500000"))
                        .monthStatus("ABUNDANCE")
                        .message("Tu es en abondance ce mois-ci.")
                        .allocations(List.of(
                                AllocationLineDto.builder().label("Besoins").amount(new BigDecimal("250000")).percentage(new BigDecimal("50")).build(),
                                AllocationLineDto.builder().label("Envies").amount(new BigDecimal("150000")).percentage(new BigDecimal("30")).build(),
                                AllocationLineDto.builder().label("Épargne").amount(new BigDecimal("100000")).percentage(new BigDecimal("20")).build()
                        ))
                        .build())
                .build();

        byte[] pdf = service.generateMonthlyPdf(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("generateMonthlyPdf tolère une liste d'allocations null")
    void generateMonthlyPdf_nullAllocations() {
        MonthlyReportData data = MonthlyReportData.builder()
                .userId(USER_ID).month(3).year(2026)
                .summary(MonthSummaryDto.builder().userId(USER_ID).month(3).year(2026)
                        .totalIncome(BigDecimal.ZERO).averageLast3Months(BigDecimal.ZERO)
                        .status("LEAN").build())
                .allocation(AllocationResultDto.builder()
                        .rule("RULE_JOSEPH").totalIncome(BigDecimal.ZERO)
                        .monthStatus("LEAN").message("Disette").allocations(null).build())
                .build();

        byte[] pdf = service.generateMonthlyPdf(data);
        assertThat(pdf).isNotEmpty();
    }

    @Test
    @DisplayName("generateAnnualPdf produit un PDF non vide")
    void generateAnnualPdf_producesPdfBytes() {
        List<AnnualMonthRow> rows = List.of(
                AnnualMonthRow.builder().month(1).totalIncome(new BigDecimal("400000")).status("NORMAL").build(),
                AnnualMonthRow.builder().month(2).totalIncome(new BigDecimal("550000")).status("ABUNDANCE").build(),
                AnnualMonthRow.builder().month(3).totalIncome(new BigDecimal("300000")).status("LEAN").build()
        );

        AnnualReportData data = AnnualReportData.builder()
                .userId(USER_ID)
                .year(2026)
                .totalAnnualIncome(new BigDecimal("1250000"))
                .abundanceMonths(1).leanMonths(1).normalMonths(1)
                .rows(rows)
                .build();

        byte[] pdf = service.generateAnnualPdf(data);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
