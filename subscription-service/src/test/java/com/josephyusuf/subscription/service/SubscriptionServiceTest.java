package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.client.AuthClient;
import com.josephyusuf.subscription.dto.PendingTransactionParams;
import com.josephyusuf.subscription.dto.SubscriptionResponse;
import com.josephyusuf.subscription.dto.TransactionResponse;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.SubscriptionStatus;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.InvalidPlanException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.mapper.SubscriptionMapper;
import com.josephyusuf.subscription.repository.SubscriptionRepository;
import com.josephyusuf.subscription.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private SubscriptionMapper mapper;
    @Mock private AuthClient authClient;

    @InjectMocks private SubscriptionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EXTERNAL_TX = "JY-aaaaaaaa-1700000000000";

    @Nested
    @DisplayName("activateAfterPayment (PayTech / Wave / Orange)")
    class ActivateTests {

        @Test
        @DisplayName("Crée une nouvelle subscription si absente")
        void activate_creates_new() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> {
                Subscription s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });
            when(transactionRepository.findByTransactionId(EXTERNAL_TX)).thenReturn(Optional.of(
                    Transaction.builder().userId(USER_ID).status(TransactionStatus.PENDING).build()));

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM,
                    PaymentProvider.WAVE, EXTERNAL_TX);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getProvider()).isEqualTo(PaymentProvider.WAVE);
            assertThat(result.isAutoRenew()).isTrue();
            assertThat(result.getExpiresAt()).isAfter(result.getStartedAt());
            verify(authClient).updatePlan(any());
            verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.SUCCEEDED));
        }

        @Test
        @DisplayName("Met à jour subscription existante (renouvellement)")
        void activate_updates_existing() {
            Subscription existing = Subscription.builder()
                    .id(UUID.randomUUID()).userId(USER_ID)
                    .plan(PlanTier.PREMIUM).status(SubscriptionStatus.EXPIRED)
                    .build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM_PLUS,
                    PaymentProvider.CARTE, null);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM_PLUS);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getCancelledAt()).isNull();
        }

        @Test
        @DisplayName("Échec AuthClient swallowed (log seul)")
        void activate_authClientFailure_swallowed() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("auth-service down")).when(authClient).updatePlan(any());

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM,
                    PaymentProvider.ORANGE_MONEY, null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("cancelSubscription / setAutoRenew")
    class CancelTests {

        @Test
        @DisplayName("cancel immediately=false → cancelAtPeriodEnd + autoRenew=false")
        void cancel_atEnd() {
            Subscription local = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .status(SubscriptionStatus.ACTIVE).autoRenew(true).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(local));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().cancelAtPeriodEnd(true).autoRenew(false).build());

            SubscriptionResponse resp = service.cancelSubscription(USER_ID, false);

            assertThat(resp.isCancelAtPeriodEnd()).isTrue();
            assertThat(resp.isAutoRenew()).isFalse();
            verify(subscriptionRepository).save(argThat(s ->
                    s.isCancelAtPeriodEnd() && !s.isAutoRenew() && s.getCancelledAt() != null));
        }

        @Test
        @DisplayName("cancel immediately=true → status CANCELLED + expiresAt now")
        void cancel_immediately() {
            Subscription local = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .status(SubscriptionStatus.ACTIVE).autoRenew(true).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(local));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().build());

            service.cancelSubscription(USER_ID, true);

            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.CANCELLED && s.getExpiresAt() != null));
        }

        @Test
        @DisplayName("cancel : pas d'abonnement → SubscriptionNotFoundException")
        void cancel_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.cancelSubscription(USER_ID, false))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("setAutoRenew(false) → flag DB pur, pas d'appel externe")
        void setAutoRenew_off() {
            Subscription sub = Subscription.builder().userId(USER_ID).autoRenew(true).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().autoRenew(false).cancelAtPeriodEnd(true).build());

            SubscriptionResponse result = service.setAutoRenew(USER_ID, false);

            assertThat(result.isAutoRenew()).isFalse();
            assertThat(result.isCancelAtPeriodEnd()).isTrue();
            verify(subscriptionRepository).save(argThat(s -> !s.isAutoRenew() && s.isCancelAtPeriodEnd()));
        }

        @Test
        @DisplayName("setAutoRenew(true) → réactive le renouvellement")
        void setAutoRenew_on() {
            Subscription sub = Subscription.builder().userId(USER_ID).autoRenew(false).cancelAtPeriodEnd(true).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().autoRenew(true).cancelAtPeriodEnd(false).build());

            service.setAutoRenew(USER_ID, true);

            verify(subscriptionRepository).save(argThat(s -> s.isAutoRenew() && !s.isCancelAtPeriodEnd()));
        }

        @Test
        @DisplayName("setAutoRenew : pas d'abonnement → SubscriptionNotFoundException")
        void setAutoRenew_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.setAutoRenew(USER_ID, true))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCurrent / getHistory / recordPendingTransaction / markTransaction*")
    class QueryTests {

        @Test
        @DisplayName("getCurrent retourne la subscription mappée avec nextInvoiceAmount XOF")
        void getCurrent_returnsMapped() {
            Subscription s = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM)
                    .currency("XOF").build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(s));
            when(mapper.toResponse(s)).thenReturn(SubscriptionResponse.builder()
                    .userId(USER_ID).plan(PlanTier.PREMIUM).build());

            SubscriptionResponse result = service.getCurrent(USER_ID);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM);
            assertThat(result.getNextInvoiceAmount()).isEqualByComparingTo("2990");
            assertThat(result.getCurrency()).isEqualTo("XOF");
        }

        @Test
        @DisplayName("getCurrent PREMIUM_PLUS → 5990 XOF")
        void getCurrent_premiumPlus() {
            Subscription s = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM_PLUS).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(s));
            when(mapper.toResponse(s)).thenReturn(SubscriptionResponse.builder().plan(PlanTier.PREMIUM_PLUS).build());

            SubscriptionResponse result = service.getCurrent(USER_ID);

            assertThat(result.getNextInvoiceAmount()).isEqualByComparingTo("5990");
            assertThat(result.getCurrency()).isEqualTo("XOF");
        }

        @Test
        @DisplayName("getCurrent FREE → pas d'enrichissement")
        void getCurrent_free() {
            Subscription s = Subscription.builder().userId(USER_ID).plan(PlanTier.FREE).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(s));
            when(mapper.toResponse(s)).thenReturn(SubscriptionResponse.builder().plan(PlanTier.FREE).build());

            SubscriptionResponse result = service.getCurrent(USER_ID);

            assertThat(result.getNextInvoiceAmount()).isNull();
        }

        @Test
        @DisplayName("getCurrent : SubscriptionNotFoundException si absent")
        void getCurrent_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getCurrent(USER_ID))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("getHistory mappe la page")
        void getHistory_mapsPage() {
            Transaction t = Transaction.builder().userId(USER_ID).build();
            Page<Transaction> page = new PageImpl<>(List.of(t));
            when(transactionRepository.findByUserIdOrderByCreatedAtDesc(eq(USER_ID), any())).thenReturn(page);
            when(mapper.toResponse(t)).thenReturn(TransactionResponse.builder().build());

            Page<TransactionResponse> result = service.getHistory(USER_ID, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("recordPendingTransaction crée tx PENDING")
        void recordPending_success() {
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
            PendingTransactionParams params = PendingTransactionParams.builder()
                    .userId(USER_ID).plan(PlanTier.PREMIUM).provider(PaymentProvider.WAVE)
                    .externalTxId("wave_1").amount(new BigDecimal("3000")).currency("XOF").build();

            Transaction tx = service.recordPendingTransaction(params);

            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.getProvider()).isEqualTo(PaymentProvider.WAVE);
        }

        @Test
        @DisplayName("recordPendingTransaction FREE → InvalidPlanException")
        void recordPending_free() {
            PendingTransactionParams params = PendingTransactionParams.builder()
                    .userId(USER_ID).plan(PlanTier.FREE).provider(PaymentProvider.WAVE)
                    .externalTxId("wave_1").amount(BigDecimal.ZERO).currency("XOF").build();
            assertThatThrownBy(() -> service.recordPendingTransaction(params))
                    .isInstanceOf(InvalidPlanException.class);
        }

        @Test
        @DisplayName("markTransactionFailed → tx FAILED + raison")
        void markFailed() {
            Transaction tx = Transaction.builder().status(TransactionStatus.PENDING).build();
            when(transactionRepository.findByTransactionId("ref")).thenReturn(Optional.of(tx));

            service.markTransactionFailed("ref", "payment refused");

            verify(transactionRepository).save(argThat(t ->
                    t.getStatus() == TransactionStatus.FAILED && "payment refused".equals(t.getFailureReason())));
        }

        @Test
        @DisplayName("markTransactionRefunded → tx REFUNDED")
        void markRefunded() {
            Transaction tx = Transaction.builder().status(TransactionStatus.SUCCEEDED).build();
            when(transactionRepository.findByTransactionId("ref")).thenReturn(Optional.of(tx));

            service.markTransactionRefunded("ref");

            verify(transactionRepository).save(argThat(t -> t.getStatus() == TransactionStatus.REFUNDED));
        }

        @Test
        @DisplayName("markRefundAndDowngrade → tx REFUNDED + plan FREE + sync auth")
        void markRefundAndDowngrade_full() {
            Transaction tx = Transaction.builder().status(TransactionStatus.SUCCEEDED).build();
            when(transactionRepository.findByTransactionId("ref")).thenReturn(Optional.of(tx));
            Subscription sub = Subscription.builder()
                    .userId(USER_ID).plan(PlanTier.PREMIUM)
                    .status(SubscriptionStatus.ACTIVE).autoRenew(true).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

            service.markRefundAndDowngrade(USER_ID, "ref");

            verify(transactionRepository).save(argThat(t -> t.getStatus() == TransactionStatus.REFUNDED));
            verify(subscriptionRepository).save(argThat(s ->
                    s.getPlan() == PlanTier.FREE
                            && s.getStatus() == SubscriptionStatus.CANCELLED
                            && s.getCancelledAt() != null
                            && !s.isAutoRenew()));
            verify(authClient).updatePlan(argThat(req -> req.getPlan() == PlanTier.FREE));
        }

        @Test
        @DisplayName("markRefundAndDowngrade sans subscription locale → tx REFUNDED + sync auth seulement")
        void markRefundAndDowngrade_noSubscription() {
            Transaction tx = Transaction.builder().status(TransactionStatus.SUCCEEDED).build();
            when(transactionRepository.findByTransactionId("ref")).thenReturn(Optional.of(tx));
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            service.markRefundAndDowngrade(USER_ID, "ref");

            verify(transactionRepository).save(argThat(t -> t.getStatus() == TransactionStatus.REFUNDED));
            verify(subscriptionRepository, never()).save(any(Subscription.class));
            verify(authClient).updatePlan(argThat(req -> req.getPlan() == PlanTier.FREE));
        }
    }
}
