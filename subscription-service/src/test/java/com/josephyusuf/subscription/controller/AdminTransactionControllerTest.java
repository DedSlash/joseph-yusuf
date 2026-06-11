package com.josephyusuf.subscription.controller;

import com.josephyusuf.subscription.dto.AdminPageResponse;
import com.josephyusuf.subscription.dto.AdminTransactionDto;
import com.josephyusuf.subscription.enums.TransactionStatus;
import com.josephyusuf.subscription.service.AdminTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTransactionController")
class AdminTransactionControllerTest {

    @Mock
    private AdminTransactionService service;

    @InjectMocks
    private AdminTransactionController controller;

    @Test
    @DisplayName("list → 200")
    void list() {
        AdminPageResponse<AdminTransactionDto> page = AdminPageResponse.<AdminTransactionDto>builder()
                .content(List.of())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(service.list(0, 20, null, null)).thenReturn(page);

        ResponseEntity<AdminPageResponse<AdminTransactionDto>> response = controller.list(0, 20, null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    @DisplayName("get → 200")
    void get() {
        UUID id = UUID.randomUUID();
        AdminTransactionDto dto = AdminTransactionDto.builder().id(id).status(TransactionStatus.SUCCEEDED).build();
        when(service.get(id)).thenReturn(dto);

        ResponseEntity<AdminTransactionDto> response = controller.get(id);

        assertThat(response.getBody().getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("refund → 200")
    void refund() {
        UUID id = UUID.randomUUID();
        AdminTransactionDto dto = AdminTransactionDto.builder().id(id).status(TransactionStatus.REFUNDED).build();
        when(service.refund(id)).thenReturn(dto);

        ResponseEntity<AdminTransactionDto> response = controller.refund(id);

        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.REFUNDED);
    }

    @Test
    @DisplayName("cancel → 200")
    void cancel() {
        UUID id = UUID.randomUUID();
        AdminTransactionDto dto = AdminTransactionDto.builder().id(id).status(TransactionStatus.CANCELLED).build();
        when(service.cancel(id)).thenReturn(dto);

        ResponseEntity<AdminTransactionDto> response = controller.cancel(id);

        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.CANCELLED);
    }

    @Test
    @DisplayName("forceActivate → 200")
    void forceActivate() {
        UUID id = UUID.randomUUID();
        AdminTransactionDto dto = AdminTransactionDto.builder().id(id).status(TransactionStatus.SUCCEEDED).build();
        when(service.forceActivate(id)).thenReturn(dto);

        ResponseEntity<AdminTransactionDto> response = controller.forceActivate(id);

        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("reconcile → 200")
    void reconcile() {
        UUID id = UUID.randomUUID();
        AdminTransactionDto dto = AdminTransactionDto.builder().id(id).status(TransactionStatus.SUCCEEDED).build();
        when(service.reconcile(id)).thenReturn(dto);

        ResponseEntity<AdminTransactionDto> response = controller.reconcile(id);

        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
    }
}
