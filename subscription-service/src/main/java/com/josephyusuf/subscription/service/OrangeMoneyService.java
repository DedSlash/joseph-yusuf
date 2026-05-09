package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.OrangeMoneyRequest;
import com.josephyusuf.subscription.dto.PaymentProviderResponse;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mode sandbox/simulation. À remplacer par les appels HTTP réels à l'API
 * Orange Money. Voir docs/PAYMENT-INTEGRATION.md.
 */
@Slf4j
@Service
public class OrangeMoneyService {

    private static final String CURRENCY_XOF = "XOF";
    private static final BigDecimal PREMIUM_AMOUNT = new BigDecimal("3000.00");
    private static final BigDecimal PREMIUM_PLUS_AMOUNT = new BigDecimal("6000.00");

    public PaymentProviderResponse initiate(UUID userId, OrangeMoneyRequest request) {
        if (request.getPlan() == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement");
        }
        BigDecimal amount = amountFor(request.getPlan());
        String simulatedTxId = "om-sim-" + UUID.randomUUID();
        log.info("[SIMULATION] OrangeMoney initiate userId={} plan={} phone={} amount={} XOF tx={}",
                userId, request.getPlan(), maskPhone(request.getPhoneNumber()), amount, simulatedTxId);

        return PaymentProviderResponse.builder()
                .provider(PaymentProvider.ORANGE_MONEY)
                .transactionId(simulatedTxId)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .currency(CURRENCY_XOF)
                .redirectUrl("https://sandbox.orange-money.com/checkout/" + simulatedTxId)
                .message("Simulation Orange Money — saisir le code OTP reçu par SMS")
                .build();
    }

    private BigDecimal amountFor(PlanTier plan) {
        return switch (plan) {
            case PREMIUM -> PREMIUM_AMOUNT;
            case PREMIUM_PLUS -> PREMIUM_PLUS_AMOUNT;
            default -> throw new InvalidPlanException("Plan non supporté : " + plan);
        };
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
