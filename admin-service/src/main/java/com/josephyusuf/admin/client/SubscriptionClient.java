package com.josephyusuf.admin.client;

import com.josephyusuf.admin.dto.PageResponse;
import com.josephyusuf.admin.dto.TransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "subscription-service")
public interface SubscriptionClient {

    @GetMapping("/api/subscriptions/admin/transactions")
    PageResponse<TransactionDto> listTransactions(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) UUID userId);

    @GetMapping("/api/subscriptions/admin/transactions/{id}")
    TransactionDto getTransaction(@PathVariable("id") UUID id);

    @PostMapping("/api/subscriptions/admin/transactions/{id}/refund")
    TransactionDto refund(@PathVariable("id") UUID id);
}
