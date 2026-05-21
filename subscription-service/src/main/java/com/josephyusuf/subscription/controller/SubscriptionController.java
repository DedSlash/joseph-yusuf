package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.CancelSubscriptionRequest;
import com.josephyusuf.subscription.dto.CreateSubscriptionRequest;
import com.josephyusuf.subscription.dto.CreateSubscriptionResponse;
import com.josephyusuf.subscription.dto.OrangeMoneyRequest;
import com.josephyusuf.subscription.dto.PaymentProviderResponse;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.SubscriptionResponse;
import com.josephyusuf.subscription.dto.TransactionResponse;
import com.josephyusuf.subscription.dto.WavePaymentRequest;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.service.OrangeMoneyService;
import com.josephyusuf.subscription.service.SubscriptionService;
import com.josephyusuf.subscription.service.WaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final WaveService waveService;
    private final OrangeMoneyService orangeMoneyService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/stripe/create")
    public ResponseEntity<CreateSubscriptionResponse> createStripeSubscription(Authentication auth,
                                                                                @Valid @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.createStripeSubscription(
                userIdOf(auth), emailOf(auth), request));
    }

    @PostMapping("/stripe/confirm/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> confirmStripeSubscription(Authentication auth,
                                                                          @PathVariable("subscriptionId") String subscriptionId) {
        return ResponseEntity.ok(subscriptionService.confirmStripeSubscription(userIdOf(auth), subscriptionId));
    }

    @DeleteMapping("/stripe/cancel")
    public ResponseEntity<SubscriptionResponse> cancelStripeSubscription(Authentication auth,
                                                                          @RequestBody(required = false) CancelSubscriptionRequest request) {
        boolean immediately = request != null && request.isImmediately();
        return ResponseEntity.ok(subscriptionService.cancelStripeSubscription(userIdOf(auth), immediately));
    }

    @PostMapping("/wave/initiate")
    public ResponseEntity<PaymentProviderResponse> initiateWave(Authentication auth,
                                                                @Valid @RequestBody WavePaymentRequest request) {
        UUID userId = userIdOf(auth);
        PaymentProviderResponse response = waveService.initiate(userId, request);
        subscriptionService.recordPendingTransaction(PendingTransactionParams.builder()
                .userId(userId)
                .plan(request.getPlan())
                .provider(PaymentProvider.WAVE)
                .externalTxId(response.getTransactionId())
                .amount(response.getAmount())
                .currency(response.getCurrency())
                .build());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orange/initiate")
    public ResponseEntity<PaymentProviderResponse> initiateOrange(Authentication auth,
                                                                  @Valid @RequestBody OrangeMoneyRequest request) {
        UUID userId = userIdOf(auth);
        PaymentProviderResponse response = orangeMoneyService.initiate(userId, request);
        subscriptionService.recordPendingTransaction(PendingTransactionParams.builder()
                .userId(userId)
                .plan(request.getPlan())
                .provider(PaymentProvider.ORANGE_MONEY)
                .externalTxId(response.getTransactionId())
                .amount(response.getAmount())
                .currency(response.getCurrency())
                .build());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> current(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getCurrent(userIdOf(auth)));
    }

    @PutMapping("/auto-renew")
    public ResponseEntity<SubscriptionResponse> setAutoRenew(Authentication auth,
                                                              @RequestParam boolean enabled) {
        return ResponseEntity.ok(subscriptionService.setAutoRenew(userIdOf(auth), enabled));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> history(Authentication auth, Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getHistory(userIdOf(auth), pageable));
    }

    private UUID userIdOf(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    private String emailOf(Authentication auth) {
        Object details = auth.getDetails();
        return details instanceof String s ? s : null;
    }
}
