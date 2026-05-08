package com.josephyusuf.ruleengine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.ruleengine.dto.*;
import com.josephyusuf.ruleengine.entity.RuleType;
import com.josephyusuf.ruleengine.security.JwtAuthenticationFilter;
import com.josephyusuf.ruleengine.security.JwtService;
import com.josephyusuf.ruleengine.service.RuleEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RuleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RuleEngineService ruleEngineService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID.toString(), "PREMIUM", List.of(new SimpleGrantedAuthority("PLAN_PREMIUM")));
    }

    @Test
    @DisplayName("POST /api/rules/calculate - 200 OK")
    void calculate_returnsOk() throws Exception {
        CalculateRequest request = CalculateRequest.builder()
                .rule(RuleType.RULE_50_30_20)
                .totalIncome(new BigDecimal("500000"))
                .build();

        AllocationResult result = AllocationResult.builder()
                .rule(RuleType.RULE_50_30_20)
                .totalIncome(new BigDecimal("500000"))
                .allocations(List.of(
                        AllocationLine.builder().category("Besoins").percentage(50).amount(new BigDecimal("250000")).build()
                ))
                .build();

        when(ruleEngineService.calculate(eq(USER_ID), eq("PREMIUM"), any(CalculateRequest.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/rules/calculate")
                        .principal(createAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rule").value("RULE_50_30_20"))
                .andExpect(jsonPath("$.allocations").isArray());
    }

    @Test
    @DisplayName("GET /api/rules/calculate/current - 200 OK")
    void calculateCurrent_returnsOk() throws Exception {
        AllocationResult result = AllocationResult.builder()
                .rule(RuleType.RULE_50_30_20)
                .totalIncome(new BigDecimal("500000"))
                .allocations(List.of())
                .build();

        when(ruleEngineService.calculateCurrent(USER_ID, "PREMIUM")).thenReturn(result);

        mockMvc.perform(get("/api/rules/calculate/current")
                        .principal(createAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rule").value("RULE_50_30_20"));
    }

    @Test
    @DisplayName("GET /api/rules/config - 200 OK")
    void getConfig_returnsOk() throws Exception {
        UserRuleConfigDto configDto = UserRuleConfigDto.builder()
                .id(UUID.randomUUID())
                .activeRule(RuleType.RULE_50_30_20)
                .josephAbundanceSavingsPercent(30)
                .josephLeanSavingsPercent(10)
                .build();

        when(ruleEngineService.getConfig(USER_ID)).thenReturn(configDto);

        mockMvc.perform(get("/api/rules/config")
                        .principal(createAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRule").value("RULE_50_30_20"))
                .andExpect(jsonPath("$.josephAbundanceSavingsPercent").value(30));
    }

    @Test
    @DisplayName("PUT /api/rules/config - 200 OK")
    void updateConfig_returnsOk() throws Exception {
        UserRuleConfigRequest request = UserRuleConfigRequest.builder()
                .activeRule(RuleType.RULE_JOSEPH)
                .josephAbundanceSavingsPercent(35)
                .josephLeanSavingsPercent(15)
                .build();

        UserRuleConfigDto configDto = UserRuleConfigDto.builder()
                .id(UUID.randomUUID())
                .activeRule(RuleType.RULE_JOSEPH)
                .josephAbundanceSavingsPercent(35)
                .josephLeanSavingsPercent(15)
                .build();

        when(ruleEngineService.updateConfig(eq(USER_ID), eq("PREMIUM"), any(UserRuleConfigRequest.class)))
                .thenReturn(configDto);

        mockMvc.perform(put("/api/rules/config")
                        .principal(createAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRule").value("RULE_JOSEPH"));
    }

    @Test
    @DisplayName("GET /api/rules/available - 200 OK")
    void getAvailableRules_returnsOk() throws Exception {
        List<RuleAvailability> rules = List.of(
                RuleAvailability.builder().rule(RuleType.RULE_50_30_20).name("50/30/20").locked(false).build(),
                RuleAvailability.builder().rule(RuleType.RULE_80_20).name("80/20").locked(false).build()
        );

        when(ruleEngineService.getAvailableRules("PREMIUM")).thenReturn(rules);

        mockMvc.perform(get("/api/rules/available")
                        .principal(createAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].rule").value("RULE_50_30_20"));
    }
}
