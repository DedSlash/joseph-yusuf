package com.josephyusuf.admin.service;

import com.josephyusuf.admin.client.AuthClient;
import com.josephyusuf.admin.dto.KpiOverviewResponse;
import com.josephyusuf.admin.dto.PlanStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class KpiService {

    private static final BigDecimal PRICE_PREMIUM_EUR = new BigDecimal("4.99");
    private static final BigDecimal PRICE_PREMIUM_PLUS_EUR = new BigDecimal("9.99");
    private static final BigDecimal PRICE_PREMIUM_XOF = new BigDecimal("3000");
    private static final BigDecimal PRICE_PREMIUM_PLUS_XOF = new BigDecimal("6000");

    private final AuthClient authClient;
    private final PromoCodeService promoCodeService;

    public KpiOverviewResponse overview() {
        PlanStatsResponse stats = authClient.planStats();

        BigDecimal mrrEur = PRICE_PREMIUM_EUR.multiply(BigDecimal.valueOf(stats.getPremium()))
                .add(PRICE_PREMIUM_PLUS_EUR.multiply(BigDecimal.valueOf(stats.getPremiumPlus())));

        BigDecimal mrrXof = PRICE_PREMIUM_XOF.multiply(BigDecimal.valueOf(stats.getPremium()))
                .add(PRICE_PREMIUM_PLUS_XOF.multiply(BigDecimal.valueOf(stats.getPremiumPlus())));

        long paying = stats.getPremium() + stats.getPremiumPlus();
        double conversionRate = stats.getTotal() > 0
                ? (double) paying / stats.getTotal() * 100.0
                : 0.0;

        return KpiOverviewResponse.builder()
                .totalUsers(stats.getTotal())
                .activeUsers(stats.getActiveUsers())
                .blockedUsers(stats.getBlockedUsers())
                .premiumUsers(stats.getPremium())
                .premiumPlusUsers(stats.getPremiumPlus())
                .freeUsers(stats.getFree())
                .mrrEur(mrrEur)
                .mrrXof(mrrXof)
                .activePromoCodes(promoCodeService.countActive())
                .conversionRate(conversionRate)
                .build();
    }
}
