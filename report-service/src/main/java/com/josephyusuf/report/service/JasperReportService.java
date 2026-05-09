package com.josephyusuf.report.service;

import com.josephyusuf.report.dto.AllocationLineDto;
import com.josephyusuf.report.dto.AnnualReportData;
import com.josephyusuf.report.dto.MonthlyReportData;
import com.josephyusuf.report.exception.ReportGenerationException;
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
public class JasperReportService {

    private static final String MONTHLY_TEMPLATE = "reports/monthly-report.jrxml";
    private static final String ANNUAL_TEMPLATE = "reports/annual-report.jrxml";

    public byte[] generateMonthlyPdf(MonthlyReportData data) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", data.getUserId().toString());
        params.put("month", data.getMonth());
        params.put("year", data.getYear());
        params.put("totalIncome", data.getSummary().getTotalIncome());
        params.put("averageLast3Months", data.getSummary().getAverageLast3Months());
        params.put("status", data.getSummary().getStatus());
        params.put("rule", data.getAllocation().getRule());
        params.put("message", data.getAllocation().getMessage());

        List<AllocationLineDto> lines = data.getAllocation().getAllocations() != null
                ? data.getAllocation().getAllocations()
                : List.of();

        return generate(MONTHLY_TEMPLATE, params, new JRBeanCollectionDataSource(lines));
    }

    public byte[] generateAnnualPdf(AnnualReportData data) {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", data.getUserId().toString());
        params.put("year", data.getYear());
        params.put("totalAnnualIncome", data.getTotalAnnualIncome());
        params.put("abundanceMonths", data.getAbundanceMonths());
        params.put("leanMonths", data.getLeanMonths());
        params.put("normalMonths", data.getNormalMonths());

        return generate(ANNUAL_TEMPLATE, params, new JRBeanCollectionDataSource(data.getRows()));
    }

    private byte[] generate(String template, Map<String, Object> params, JRBeanCollectionDataSource dataSource) {
        try (InputStream stream = new ClassPathResource(template).getInputStream()) {
            JasperReport jasperReport = JasperCompileManager.compileReport(stream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            log.error("Échec génération PDF template={} : {}", template, e.getMessage());
            throw new ReportGenerationException("Échec génération PDF: " + e.getMessage(), e);
        }
    }
}
