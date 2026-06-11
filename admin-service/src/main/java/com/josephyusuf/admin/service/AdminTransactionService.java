package com.josephyusuf.admin.service;

import com.josephyusuf.admin.client.SubscriptionClient;
import com.josephyusuf.admin.dto.PageResponse;
import com.josephyusuf.admin.dto.TransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private final SubscriptionClient subscriptionClient;
    private final AuditLogService auditLogService;

    public PageResponse<TransactionDto> list(int page, int size, String status, UUID userId) {
        return subscriptionClient.listTransactions(page, size, status, userId);
    }

    public TransactionDto get(UUID id) {
        return subscriptionClient.getTransaction(id);
    }

    public TransactionDto refund(UUID id, UUID adminId, String ip) {
        TransactionDto refunded = subscriptionClient.refund(id);
        auditLogService.log(adminId, "TRANSACTION_REFUND", "TRANSACTION", id.toString(), null, ip);
        return refunded;
    }

    public TransactionDto cancel(UUID id, UUID adminId, String ip) {
        TransactionDto cancelled = subscriptionClient.cancel(id);
        auditLogService.log(adminId, "TRANSACTION_CANCEL", "TRANSACTION", id.toString(), null, ip);
        return cancelled;
    }

    public TransactionDto forceActivate(UUID id, UUID adminId, String ip) {
        TransactionDto activated = subscriptionClient.forceActivate(id);
        auditLogService.log(adminId, "TRANSACTION_FORCE_ACTIVATE", "TRANSACTION", id.toString(), null, ip);
        return activated;
    }

    public TransactionDto reconcile(UUID id, UUID adminId, String ip) {
        TransactionDto reconciled = subscriptionClient.reconcile(id);
        auditLogService.log(adminId, "TRANSACTION_RECONCILE", "TRANSACTION", id.toString(), null, ip);
        return reconciled;
    }
}
