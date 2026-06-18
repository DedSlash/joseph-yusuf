package com.josephyusuf.report.service;

import com.josephyusuf.report.dto.AllocationLineDto;
import com.josephyusuf.report.dto.AnnualMonthRow;
import com.josephyusuf.report.dto.AnnualReportData;
import com.josephyusuf.report.dto.MonthlyReportData;
import com.josephyusuf.report.exception.ReportGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JasperReportService {

    private final ReportCurrencyConverter currencyConverter;

    private static final String MONTHLY_TEMPLATE = "reports/monthly-report.jrxml";
    private static final String ANNUAL_TEMPLATE = "reports/annual-report.jrxml";

    private static String translateStatus(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "ABUNDANCE" -> "Abondance";
            case "LEAN"      -> "Disette";
            case "NORMAL"    -> "Normal";
            default          -> status;
        };
    }

    private static String translateRule(String rule) {
        if (rule == null) return "—";
        return switch (rule) {
            case "RULE_50_30_20" -> "Règle 50/30/20";
            case "RULE_80_20"    -> "Règle 80/20 (Pareto)";
            case "RULE_70_20_10" -> "Règle 70/20/10";
            case "RULE_JOSEPH"   -> "Principe de Joseph";
            default              -> rule;
        };
    }

    public byte[] generateMonthlyPdf(MonthlyReportData data) {
        String currency = data.getDisplayCurrency() != null ? data.getDisplayCurrency() : "XOF";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", data.getUserId().toString());
        params.put("month", data.getMonth());
        params.put("year", data.getYear());
        params.put("totalIncome", currencyConverter.fromXOF(data.getSummary().getTotalIncome(), currency));
        params.put("averageLast3Months", currencyConverter.fromXOF(data.getSummary().getAverageLast3Months(), currency));
        params.put("status", translateStatus(data.getSummary().getStatus()));
        params.put("rule", translateRule(String.valueOf(data.getAllocation().getRule())));
        String message = data.getAllocation().getMessage();
        params.put("message", message != null ? message : "");
        params.put("currencyCode", currencyConverter.displayCode(currency));

        List<AllocationLineDto> lines = data.getAllocation().getAllocations() != null
                ? data.getAllocation().getAllocations().stream()
                    .map(line -> AllocationLineDto.builder()
                            .label(line.getLabel())
                            .amount(currencyConverter.fromXOF(line.getAmount(), currency))
                            .percentage(line.getPercentage())
                            .build())
                    .toList()
                : List.<AllocationLineDto>of();

        return generate(MONTHLY_TEMPLATE, params, new JRBeanCollectionDataSource(lines));
    }

    public byte[] generateAnnualPdf(AnnualReportData data) {
        String currency = data.getDisplayCurrency() != null ? data.getDisplayCurrency() : "XOF";

        Map<String, Object> params = new HashMap<>();
        params.put("userId", data.getUserId().toString());
        params.put("year", data.getYear());
        params.put("totalAnnualIncome", currencyConverter.fromXOF(data.getTotalAnnualIncome(), currency));
        params.put("abundanceMonths", data.getAbundanceMonths());
        params.put("leanMonths", data.getLeanMonths());
        params.put("normalMonths", data.getNormalMonths());
        params.put("currencyCode", currencyConverter.displayCode(currency));

        List<AnnualMonthRow> translatedRows = data.getRows().stream()
                .map(r -> AnnualMonthRow.builder()
                        .month(r.getMonth())
                        .totalIncome(currencyConverter.fromXOF(r.getTotalIncome(), currency))
                        .status(translateStatus(r.getStatus()))
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return generate(ANNUAL_TEMPLATE, params, new JRBeanCollectionDataSource(translatedRows));
    }

    private byte[] generate(String template, Map<String, Object> params, JRBeanCollectionDataSource dataSource) {
        try (InputStream stream = new ClassPathResource(template).getInputStream()) {
            JasperReport jasperReport = JasperCompileManager.compileReport(stream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            log.error("Échec génération PDF template={} : {}", template, e.getMessage());
            throw new ReportGenerationException("La génération du rapport PDF a échoué. Veuillez réessayer.", e);
        }
    }
}
