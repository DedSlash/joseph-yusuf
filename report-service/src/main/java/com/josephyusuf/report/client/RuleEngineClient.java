package com.josephyusuf.report.client;

import com.josephyusuf.report.dto.AllocationResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "rule-engine-service")
public interface RuleEngineClient {

    @GetMapping("/api/rules/result")
    AllocationResultDto getResult(@RequestParam int month, @RequestParam int year);
}
