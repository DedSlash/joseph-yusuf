package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.OrangeMoneyRequest;
import com.josephyusuf.subscription.dto.PaymentIntentRequest;
import com.josephyusuf.subscription.dto.PaymentIntentResponse;
import com.josephyusuf.subscription.dto.PaymentProviderResponse;
import com.josephyusuf.subscription.dto.SubscriptionResponse;
import com.josephyusuf.subscription.dto.TransactionResponse;
import com.josephyusuf.subscription.dto.WavePaymentRequest;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.service.OrangeMoneyService;
import com.josephyusuf.subscription.service.StripeService;
import com.josephyusuf.subscription.service.SubscriptionService;
import com.josephyusuf.subscription.service.WaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final StripeService stripeService;
    private final WaveService waveService;
    private final OrangeMoneyService orangeMoneyService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/stripe/create-payment-intent")
    public ResponseEntity<PaymentIntentResponse> createStripePaymentIntent(Authentication auth,
                                                                           @Valid @RequestBody PaymentIntentRequest request) {
        UUID userId = userIdOf(auth);
        PaymentIntentResponse response = stripeService.createPaymentIntent(userId, request.getPlan(),
                request.getCurrency(), request.getPromoCode());
        subscriptionService.recordPendingTransaction(userId, request.getPlan(), PaymentProvider.STRIPE,
                response.getPaymentIntentId(), response.getAmount(), response.getCurrency());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/wave/initiate")
    public ResponseEntity<PaymentProviderResponse> initiateWave(Authentication auth,
                                                                @Valid @RequestBody WavePaymentRequest request) {
        UUID userId = userIdOf(auth);
        PaymentProviderResponse response = waveService.initiate(userId, request);
        subscriptionService.recordPendingTransaction(userId, request.getPlan(), PaymentProvider.WAVE,
                response.getTransactionId(), response.getAmount(), response.getCurrency());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/orange/initiate")
    public ResponseEntity<PaymentProviderResponse> initiateOrange(Authentication auth,
                                                                  @Valid @RequestBody OrangeMoneyRequest request) {
        UUID userId = userIdOf(auth);
        PaymentProviderResponse response = orangeMoneyService.initiate(userId, request);
        subscriptionService.recordPendingTransaction(userId, request.getPlan(), PaymentProvider.ORANGE_MONEY,
                response.getTransactionId(), response.getAmount(), response.getCurrency());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current")
    public ResponseEntity<SubscriptionResponse> current(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getCurrent(userIdOf(auth)));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> history(Authentication auth, Pageable pageable) {
        return ResponseEntity.ok(subscriptionService.getHistory(userIdOf(auth), pageable));
    }

    private UUID userIdOf(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }
}
