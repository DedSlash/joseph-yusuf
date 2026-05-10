package com.josephyusuf.admin.controller;

import com.josephyusuf.admin.dto.KpiOverviewResponse;
import com.josephyusuf.admin.dto.PlanStatsResponse;
import com.josephyusuf.admin.service.AdminUserService;
import com.josephyusuf.admin.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;
    private final AdminUserService adminUserService;

    @GetMapping("/overview")
    public ResponseEntity<KpiOverviewResponse> overview() {
        return ResponseEntity.ok(kpiService.overview());
    }

    @GetMapping("/plans")
    public ResponseEntity<PlanStatsResponse> plans() {
        return ResponseEntity.ok(adminUserService.planStats());
    }
}
