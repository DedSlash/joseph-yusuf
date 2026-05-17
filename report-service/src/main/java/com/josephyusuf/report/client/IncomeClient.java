package com.josephyusuf.report.client;

import com.josephyusuf.report.dto.MonthSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "income-service", configuration = com.josephyusuf.report.config.FeignConfig.class)
public interface IncomeClient {

    @GetMapping("/api/incomes/summary")
    MonthSummaryDto getSummary(@RequestParam int month, @RequestParam int year);
}
