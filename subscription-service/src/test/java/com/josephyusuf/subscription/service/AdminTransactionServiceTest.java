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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private AdminTransactionService service;

    private Transaction sampleTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .provider(PaymentProvider.WAVE)
                .transactionId("JY-aaaaaaaa-1700000000000")
                .amount(BigDecimal.valueOf(2990))
                .currency("XOF")
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
        assertThat(response.getContent().get(0).getProviderTransactionId()).isEqualTo("JY-aaaaaaaa-1700000000000");
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
        assertThat(dto.getProviderTransactionId()).isEqualTo("JY-aaaaaaaa-1700000000000");
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
    @DisplayName("refund - status non SUCCEEDED → PaymentException, pas de persistance")
    void refund_notSucceeded() {
        Transaction tx = sampleTransaction(TransactionStatus.PENDING);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.refund(tx.getId()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("SUCCEEDED");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("refund - SUCCEEDED → marquage local REFUNDED, persistance et DTO renvoyés")
    void refund_marksLocalRefunded() {
        Transaction tx = sampleTransaction(TransactionStatus.SUCCEEDED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        AdminTransactionDto dto = service.refund(tx.getId());

        assertThat(dto.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        verify(transactionRepository).save(tx);
    }

    @Test
    @DisplayName("cancel - status PENDING → CANCELLED")
    void cancel_pending() {
        Transaction tx = sampleTransaction(TransactionStatus.PENDING);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        AdminTransactionDto dto = service.cancel(tx.getId());

        assertThat(dto.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        verify(transactionRepository).save(tx);
    }

    @Test
    @DisplayName("cancel - status FAILED → CANCELLED")
    void cancel_failed() {
        Transaction tx = sampleTransaction(TransactionStatus.FAILED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        AdminTransactionDto dto = service.cancel(tx.getId());

        assertThat(dto.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        verify(transactionRepository).save(tx);
    }

    @Test
    @DisplayName("cancel - status SUCCEEDED → PaymentException")
    void cancel_succeeded_rejected() {
        Transaction tx = sampleTransaction(TransactionStatus.SUCCEEDED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.cancel(tx.getId()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("PENDING ou FAILED");
    }

    @Test
    @DisplayName("forceActivate - PENDING → active l'abonnement")
    void forceActivate_pending() {
        Transaction tx = sampleTransaction(TransactionStatus.PENDING);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        service.forceActivate(tx.getId());

        verify(subscriptionService).activateAfterPayment(
                tx.getUserId(), tx.getPlan(), tx.getProvider(), tx.getTransactionId());
    }

    @Test
    @DisplayName("forceActivate - REFUNDED → PaymentException")
    void forceActivate_refunded_rejected() {
        Transaction tx = sampleTransaction(TransactionStatus.REFUNDED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.forceActivate(tx.getId()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("ne peut pas être activée");
    }

    @Test
    @DisplayName("forceActivate - CANCELLED → PaymentException")
    void forceActivate_cancelled_rejected() {
        Transaction tx = sampleTransaction(TransactionStatus.CANCELLED);
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.forceActivate(tx.getId()))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("ne peut pas être activée");
    }
}
