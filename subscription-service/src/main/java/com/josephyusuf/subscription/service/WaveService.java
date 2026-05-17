package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.PaymentProviderResponse;
import com.josephyusuf.subscription.dto.WavePaymentRequest;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mode sandbox/simulation. À remplacer par les appels HTTP réels à l'API Wave.
 * Voir docs/PAYMENT-INTEGRATION.md pour la procédure de branchement.
 */
@Slf4j
@Service
public class WaveService {

    private static final String CURRENCY_XOF = "XOF";
    private static final BigDecimal PREMIUM_AMOUNT = new BigDecimal("3000.00");
    private static final BigDecimal PREMIUM_PLUS_AMOUNT = new BigDecimal("6000.00");

    public PaymentProviderResponse initiate(UUID userId, WavePaymentRequest request) {
        if (request.getPlan() == PlanTier.FREE) {
            throw new InvalidPlanException("Le plan FREE ne nécessite pas de paiement");
        }
        BigDecimal amount = amountFor(request.getPlan());
        String simulatedTxId = "wave-sim-" + UUID.randomUUID();
        log.info("[SIMULATION] Wave initiate userId={} plan={} phone={} amount={} XOF tx={}",
                userId, request.getPlan(), maskPhone(request.getPhoneNumber()), amount, simulatedTxId);

        return PaymentProviderResponse.builder()
                .provider(PaymentProvider.WAVE)
                .transactionId(simulatedTxId)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .currency(CURRENCY_XOF)
                .redirectUrl("https://sandbox.wave.com/checkout/" + simulatedTxId)
                .message("Simulation Wave — confirmer le paiement sur le téléphone")
                .build();
    }

    private BigDecimal amountFor(PlanTier plan) {
        return switch (plan) {
            case PREMIUM -> PREMIUM_AMOUNT;
            case PREMIUM_PLUS -> PREMIUM_PLUS_AMOUNT;
            default -> throw new InvalidPlanException("Ce plan ne peut pas être souscrit via ce mode de paiement.");
        };
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }
}
