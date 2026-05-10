package com.josephyusuf.admin.service;

import com.josephyusuf.admin.client.SubscriptionClient;
import com.josephyusuf.admin.dto.PageResponse;
import com.josephyusuf.admin.dto.TransactionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceTest {

    @Mock private SubscriptionClient subscriptionClient;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private AdminTransactionService service;

    @Test
    @DisplayName("list - delegates to subscriptionClient")
    void list_delegates() {
        UUID userId = UUID.randomUUID();
        PageResponse<TransactionDto> page = PageResponse.<TransactionDto>builder().build();
        when(subscriptionClient.listTransactions(0, 20, "SUCCEEDED", userId)).thenReturn(page);

        assertThat(service.list(0, 20, "SUCCEEDED", userId)).isSameAs(page);
        verify(subscriptionClient).listTransactions(0, 20, "SUCCEEDED", userId);
    }

    @Test
    @DisplayName("get - delegates to subscriptionClient")
    void get_delegates() {
        UUID id = UUID.randomUUID();
        TransactionDto tx = TransactionDto.builder().id(id).build();
        when(subscriptionClient.getTransaction(id)).thenReturn(tx);

        assertThat(service.get(id)).isSameAs(tx);
    }

    @Test
    @DisplayName("refund - delegates and writes audit log")
    void refund_audited() {
        UUID id = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        TransactionDto refunded = TransactionDto.builder().id(id).status("REFUNDED").build();
        when(subscriptionClient.refund(id)).thenReturn(refunded);

        TransactionDto result = service.refund(id, adminId, "10.0.0.1");

        assertThat(result).isSameAs(refunded);
        verify(auditLogService).log(eq(adminId), eq("TRANSACTION_REFUND"), eq("TRANSACTION"),
                eq(id.toString()), isNull(), eq("10.0.0.1"));
    }
}
