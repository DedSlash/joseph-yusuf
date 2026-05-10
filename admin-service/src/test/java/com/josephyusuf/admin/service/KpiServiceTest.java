package com.josephyusuf.admin.service;

import com.josephyusuf.admin.client.AuthClient;
import com.josephyusuf.admin.dto.KpiOverviewResponse;
import com.josephyusuf.admin.dto.PlanStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiServiceTest {

    @Mock
    private AuthClient authClient;

    @Mock
    private PromoCodeService promoCodeService;

    @InjectMocks
    private KpiService kpiService;

    @Test
    @DisplayName("overview - computes MRR and conversion rate")
    void overview_computesKpis() {
        when(authClient.planStats()).thenReturn(PlanStatsResponse.builder()
                .free(70).premium(20).premiumPlus(10).total(100)
                .activeUsers(95).blockedUsers(5).admins(2).build());
        when(promoCodeService.countActive()).thenReturn(3L);

        KpiOverviewResponse result = kpiService.overview();

        assertThat(result.getTotalUsers()).isEqualTo(100);
        assertThat(result.getConversionRate()).isEqualTo(30.0);
        assertThat(result.getMrrEur()).isEqualByComparingTo(new BigDecimal("199.70"));
        assertThat(result.getActivePromoCodes()).isEqualTo(3);
    }

    @Test
    @DisplayName("overview - handles zero total users")
    void overview_zeroUsers() {
        when(authClient.planStats()).thenReturn(PlanStatsResponse.builder()
                .free(0).premium(0).premiumPlus(0).total(0).build());
        when(promoCodeService.countActive()).thenReturn(0L);

        KpiOverviewResponse result = kpiService.overview();

        assertThat(result.getConversionRate()).isZero();
    }
}
