package com.josephyusuf.subscription.service;

import com.josephyusuf.subscription.dto.AdminPageResponse;
import com.josephyusuf.subscription.dto.AdminTransactionDto;
import com.josephyusuf.subscription.entity.Transaction;
import com.josephyusuf.subscription.enums.PaymentProvider;
import com.josephyusuf.subscription.enums.PlanTier;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.exception.PaymentException;
import com.josephyusuf.subscription.exception.SubscriptionNotFoundException;
import com.josephyusuf.subscription.repository.TransactionRepository;
import com.stripe.exception.ApiException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
class AdminTransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private AdminTransactionService service;

    private MockedStatic<Refund> refundMock;

    @BeforeEach
    void setUp() {
        refundMock = mockStatic(Refund.class);
    }

    @AfterEach
    void tearDown() {
        refundMock.close();
    }

    private Transaction sampleTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .provider(PaymentProvider.STRIPE)
                .transactionId("pi_123")
                .amount(BigDecimal.valueOf(4.99))
                .currency("EUR")
                .plan(PlanTier.PREMIUM)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("list - sans filtres, mappe la page repository en AdminPageResponse")
    void list_noFilters() {
        Transaction tx = sampleTransaction(TransactionStatus.SUCCEEDED);
        Page<Transaction> page = new PageImpl<>(List.of(tx));
        when(transactionRepository.findAllForAdmin(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

        AdminPageResponse<AdminTransactionDto> response = service.list(0, 20, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getProviderTransactionId()).isEqualTo("pi_123");
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("list - avec filtre status valide, le passe à findAllForAdmin")
    void list_withStatusFilter() {
        UUID userId = UUID.randomUUID();
        when(transactionRepository.findAllForAdmin(eq(TransactionStatus.SUCCEEDED), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AdminPageResponse<AdminTransactionDto> response = service.list(0, 20, "succeeded", userId);

        assertThat(response.getContent()).isEmpty();
        verify(transactionRepository).findAllForAdmin(eq(TransactionStatus.SUCCEEDED), eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("list - status invalide est traité comme null")
    void list_invalidStatus() {
        when(transactionRepository.findAllForAdmin(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(0, 20, "BOGUS", null);

        verify(transactionRepository).findAllForAdmin(eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("get - existant → DTO")
    void get_found() {
        Transaction tx = sampleTransaction(TransactionStatus.SUCCEEDED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        AdminTransactionDto dto = service.get(tx.getId());

        assertThat(dto.getId()).isEqualTo(tx.getId());
        assertThat(dto.getProviderTransactionId()).isEqualTo("pi_123");
    }

    @Test
    @DisplayName("get - introuvable → SubscriptionNotFoundException")
    void get_notFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining("Transaction introuvable");
    }

    @Test
    @DisplayName("refund - status non SUCCEEDED → PaymentException, pas d'appel Stripe")
    void refund_notSucceeded() {
        Transaction tx = sampleTransaction(TransactionStatus.PENDING);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.refund(tx.getId()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("SUCCEEDED");

        refundMock.verifyNoInteractions();
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("refund - succès Stripe → status passe à REFUNDED, persistance et DTO renvoyés")
    void refund_success() {
        Transaction tx = sampleTransaction(TransactionStatus.SUCCEEDED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        Refund refund = mock(Refund.class);
        refundMock.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(refund);

        AdminTransactionDto dto = service.refund(tx.getId());

        assertThat(dto.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        verify(transactionRepository).save(tx);
    }

    @Test
    @DisplayName("refund - StripeException → PaymentException, status inchangé")
    void refund_stripeFails() {
        Transaction tx = sampleTransaction(TransactionStatus.SUCCEEDED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        refundMock.when(() -> Refund.create(any(RefundCreateParams.class)))
                .thenThrow(new ApiException("API down", "req_1", "api_error", 500, null));

        assertThatThrownBy(() -> service.refund(tx.getId()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("Échec remboursement Stripe");

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        verify(transactionRepository, never()).save(any());
    }
}
