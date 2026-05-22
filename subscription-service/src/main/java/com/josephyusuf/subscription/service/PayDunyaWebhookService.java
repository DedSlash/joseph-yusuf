package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AdminClient;
import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.dto.PayDunyaStatusResponse;
import com.josephyusuf.subscription.dto.PlanUpdateRequest;
import com.josephyusuf.subscription.dto.PromoCodeApplyRequest;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayDunyaWebhookService {

    private final PayDunyaService payDunyaService;
    private final SubscriptionService subscriptionService;
    private final AuthClient authClient;
    private final AdminClient adminClient;

    @Transactional
    public void handleCallback(Map<String, Object> payload) {
        Object dataObj = payload.get("data");
        Map<String, Object> data = dataObj instanceof Map ? (Map<String, Object>) dataObj : payload;

        String token = extractToken(data);
        if (token == null) {
            log.warn("PayDunya callback sans token, ignoré");
            return;
        }

        PayDunyaStatusResponse status = payDunyaService.checkInvoiceStatus(token);

        if ("completed".equals(status.getStatus())) {
            processCompletedPayment(token, status.getCustomData());
        } else if ("cancelled".equals(status.getStatus()) || "failed".equals(status.getStatus())) {
            subscriptionService.markTransactionFailed(token,
                    "PayDunya paiement " + status.getStatus());
            log.info("PayDunya paiement {} token={}", status.getStatus(), token);
        } else {
            log.info("PayDunya callback statut={} token={} — rien à faire", status.getStatus(), token);
        }
    }

    private void processCompletedPayment(String token, Map<String, Object> customData) {
        if (customData == null) {
            log.error("PayDunya completed mais pas de custom_data, token={}", token);
            return;
        }

        String userId = String.valueOf(customData.get("userId"));
        String planTier = String.valueOf(customData.get("planTier"));
        Object couponObj = customData.get("couponCode");
        String couponCode = couponObj != null && !"null".equals(String.valueOf(couponObj))
                ? String.valueOf(couponObj) : null;

        PlanTier plan = PlanTier.valueOf(planTier);
        UUID userUuid = UUID.fromString(userId);

        subscriptionService.activateAfterPayment(
                userUuid, plan, PaymentProvider.PAYDUNYA, token);

        if (couponCode != null) {
            try {
                adminClient.apply(PromoCodeApplyRequest.builder()
                        .code(couponCode)
                        .userId(userUuid)
                        .build());
            } catch (Exception e) {
                log.warn("Impossible d'enregistrer usage promo {} pour user={} : {}",
                        couponCode, userId, e.getMessage());
            }
        }

        authClient.updatePlan(PlanUpdateRequest.builder()
                .userId(userUuid)
                .plan(plan)
                .build());

        log.info("PayDunya paiement confirmé et activé userId={} plan={} token={}",
                userId, planTier, token);
    }

    private String extractToken(Map<String, Object> data) {
        Object tokenObj = data.get("token");
        if (tokenObj != null) {
            return String.valueOf(tokenObj);
        }
        Object invoiceObj = data.get("invoice");
        if (invoiceObj instanceof Map) {
            Object t = ((Map<String, Object>) invoiceObj).get("token");
            if (t != null) return String.valueOf(t);
        }
        return null;
    }
}
