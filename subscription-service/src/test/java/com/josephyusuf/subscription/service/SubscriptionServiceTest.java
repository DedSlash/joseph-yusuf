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
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void injectSelf() {
        service.setSelf(service);
    }

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EXTERNAL_TX = "pi_test_123";

    @Nested
    @DisplayName("recordPendingTransaction")
    class RecordPendingTests {

        @Test
        @DisplayName("Crée une transaction PENDING pour PREMIUM")
        void recordPending_premium_success() {
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            PendingTransactionParams params = PendingTransactionParams.builder()
                    .userId(USER_ID).plan(PlanTier.PREMIUM).provider(PaymentProvider.STRIPE)
                    .externalTxId(EXTERNAL_TX).amount(new BigDecimal("499")).currency("EUR")
                    .build();
            Transaction tx = service.recordPendingTransaction(params);

            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.getProvider()).isEqualTo(PaymentProvider.STRIPE);
            assertThat(tx.getTransactionId()).isEqualTo(EXTERNAL_TX);
        }

        @Test
        @DisplayName("FREE → InvalidPlanException")
        void recordPending_free_throws() {
            PendingTransactionParams params = PendingTransactionParams.builder()
                    .userId(USER_ID).plan(PlanTier.FREE).provider(PaymentProvider.STRIPE)
                    .externalTxId(EXTERNAL_TX).amount(BigDecimal.ZERO).currency("EUR")
                    .build();

            assertThatThrownBy(() -> service.recordPendingTransaction(params))
                    .isInstanceOf(InvalidPlanException.class);
        }
    }

    @Nested
    @DisplayName("activateAfterPayment")
    class ActivateTests {

        @Test
        @DisplayName("Crée une nouvelle subscription si l'utilisateur n'en a pas")
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
                    PaymentProvider.STRIPE, EXTERNAL_TX);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            verify(authClient).updatePlan(any());
            verify(transactionRepository).save(argThat(tx -> tx.getStatus() == TransactionStatus.SUCCEEDED));
        }

        @Test
        @DisplayName("Met à jour la subscription existante")
        void activate_updates_existing() {
            Subscription existing = Subscription.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).plan(PlanTier.PREMIUM)
                    .status(SubscriptionStatus.CANCELLED).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.findByTransactionId(EXTERNAL_TX)).thenReturn(Optional.empty());

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM_PLUS,
                    PaymentProvider.STRIPE, EXTERNAL_TX);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM_PLUS);
            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(result.getCancelledAt()).isNull();
        }

        @Test
        @DisplayName("L'échec d'AuthClient ne casse pas l'activation")
        void activate_authClientFailure_swallowed() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("auth-service down")).when(authClient).updatePlan(any());

            Subscription result = service.activateAfterPayment(USER_ID, PlanTier.PREMIUM,
                    PaymentProvider.STRIPE, null);

            assertThat(result).isNotNull();
            verify(authClient).updatePlan(any());
        }
    }

    @Nested
    @DisplayName("markTransactionFailed / Refunded")
    class MarkTransactionTests {

        @Test
        @DisplayName("Marque FAILED avec raison")
        void markFailed_setsReason() {
            Transaction tx = Transaction.builder().userId(USER_ID).status(TransactionStatus.PENDING).build();
            when(transactionRepository.findByTransactionId(EXTERNAL_TX)).thenReturn(Optional.of(tx));

            service.markTransactionFailed(EXTERNAL_TX, "carte refusée");

            verify(transactionRepository).save(argThat(t ->
                    t.getStatus() == TransactionStatus.FAILED && "carte refusée".equals(t.getFailureReason())));
        }

        @Test
        @DisplayName("Marque REFUNDED")
        void markRefunded() {
            Transaction tx = Transaction.builder().userId(USER_ID).status(TransactionStatus.SUCCEEDED).build();
            when(transactionRepository.findByTransactionId(EXTERNAL_TX)).thenReturn(Optional.of(tx));

            service.markTransactionRefunded(EXTERNAL_TX);

            verify(transactionRepository).save(argThat(t -> t.getStatus() == TransactionStatus.REFUNDED));
        }

        @Test
        @DisplayName("Transaction inconnue : no-op silencieux")
        void markFailed_unknownTx_silent() {
            when(transactionRepository.findByTransactionId(EXTERNAL_TX)).thenReturn(Optional.empty());

            service.markTransactionFailed(EXTERNAL_TX, "x");

            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancel / setAutoRenew")
    class CancelTests {

        @Test
        @DisplayName("cancel - ACTIVE → CANCELLED")
        void cancel_active() {
            Subscription sub = Subscription.builder().userId(USER_ID)
                    .plan(PlanTier.PREMIUM).status(SubscriptionStatus.ACTIVE).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Subscription result = service.cancel(USER_ID);

            assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(result.getCancelledAt()).isNotNull();
            verify(authClient).updatePlan(any());
        }

        @Test
        @DisplayName("cancel - déjà annulé → InvalidPlanException")
        void cancel_alreadyCancelled() {
            Subscription sub = Subscription.builder().userId(USER_ID)
                    .status(SubscriptionStatus.CANCELLED).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));

            assertThatThrownBy(() -> service.cancel(USER_ID))
                    .isInstanceOf(InvalidPlanException.class)
                    .hasMessageContaining("déjà annulé");
        }

        @Test
        @DisplayName("cancel - pas d'abonnement → SubscriptionNotFoundException")
        void cancel_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancel(USER_ID))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        @Test
        @DisplayName("setAutoRenew - existant → met à jour")
        void setAutoRenew_success() {
            Subscription sub = Subscription.builder().userId(USER_ID)
                    .plan(PlanTier.PREMIUM).autoRenew(false).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(Subscription.class)))
                    .thenReturn(SubscriptionResponse.builder().autoRenew(true).build());

            SubscriptionResponse result = service.setAutoRenew(USER_ID, true);

            assertThat(result.isAutoRenew()).isTrue();
        }

        @Test
        @DisplayName("setAutoRenew - pas d'abonnement → SubscriptionNotFoundException")
        void setAutoRenew_notFound() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setAutoRenew(USER_ID, true))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getCurrent / getHistory")
    class QueryTests {

        @Test
        @DisplayName("getCurrent retourne la subscription mappée")
        void getCurrent_returnsMapped() {
            Subscription s = Subscription.builder().userId(USER_ID).plan(PlanTier.PREMIUM).build();
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(s));
            when(mapper.toResponse(s)).thenReturn(SubscriptionResponse.builder().userId(USER_ID).plan(PlanTier.PREMIUM).build());

            SubscriptionResponse result = service.getCurrent(USER_ID);

            assertThat(result.getPlan()).isEqualTo(PlanTier.PREMIUM);
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
    }
}
