package com.josephyusuf.ruleengine.client;

import com.josephyusuf.ruleengine.dto.MonthSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "income-service")
public interface IncomeClient {

    @GetMapping("/api/incomes/summary")
    MonthSummaryResponse getSummary(@RequestParam int month, @RequestParam int year);
}
