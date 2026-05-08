package com.josephyusuf.income.controller;

import com.josephyusuf.income.dto.MonthSummary;
import com.josephyusuf.income.entity.MonthStatus;
import com.josephyusuf.income.service.MonthSummaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeSummaryControllerTest {

    @Mock
    private MonthSummaryService summaryService;

    @InjectMocks
    private IncomeSummaryController controller;

    private static final UUID USER_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID.toString(), "FREE", List.of());
    }

    @Test
    @DisplayName("getSummary - returns 200")
    void getSummary_returns200() {
        MonthSummary summary = MonthSummary.builder()
                .userId(USER_ID).month(5).year(2026)
                .totalIncome(new BigDecimal("500000"))
                .status(MonthStatus.NORMAL).build();

        when(summaryService.getSummary(USER_ID, 5, 2026)).thenReturn(summary);

        ResponseEntity<MonthSummary> response = controller.getSummary(createAuth(), 5, 2026);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalIncome()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("getHistory - returns 200")
    void getHistory_returns200() {
        MonthSummary summary = MonthSummary.builder()
                .userId(USER_ID).month(5).year(2026)
                .totalIncome(new BigDecimal("500000"))
                .status(MonthStatus.NORMAL).build();

        when(summaryService.getHistory(USER_ID, 12)).thenReturn(List.of(summary));

        ResponseEntity<List<MonthSummary>> response = controller.getHistory(createAuth(), 12);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
