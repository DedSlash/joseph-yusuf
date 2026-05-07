package com.josephyusuf.ruleengine.controller;

import com.josephyusuf.ruleengine.dto.*;
import com.josephyusuf.ruleengine.service.RuleEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleEngineService ruleEngineService;

    @PostMapping("/calculate")
    public ResponseEntity<AllocationResult> calculate(Authentication auth,
                                                      @Valid @RequestBody CalculateRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(ruleEngineService.calculate(userId, plan, request));
    }

    @GetMapping("/calculate/current")
    public ResponseEntity<AllocationResult> calculateCurrent(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(ruleEngineService.calculateCurrent(userId, plan));
    }

    @GetMapping("/config")
    public ResponseEntity<UserRuleConfigDto> getConfig(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        return ResponseEntity.ok(ruleEngineService.getConfig(userId));
    }

    @PutMapping("/config")
    public ResponseEntity<UserRuleConfigDto> updateConfig(Authentication auth,
                                                           @Valid @RequestBody UserRuleConfigRequest request) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(ruleEngineService.updateConfig(userId, plan, request));
    }

    @GetMapping("/available")
    public ResponseEntity<List<RuleAvailability>> getAvailableRules(Authentication auth) {
        String plan = (String) auth.getCredentials();
        return ResponseEntity.ok(ruleEngineService.getAvailableRules(plan));
    }
}
