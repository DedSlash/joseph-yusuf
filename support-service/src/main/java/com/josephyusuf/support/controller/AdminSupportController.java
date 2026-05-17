package com.josephyusuf.support.controller;

import com.josephyusuf.support.dto.*;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.enums.TicketStatus;
import com.josephyusuf.support.service.KnowledgeService;
import com.josephyusuf.support.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/support/admin")
@RequiredArgsConstructor
public class AdminSupportController {

    private final TicketService ticketService;
    private final KnowledgeService knowledgeService;

    @GetMapping("/tickets")
    public ResponseEntity<PageResponse<TicketDto>> listAllTickets(@RequestParam(required = false) TicketStatus status,
                                                                   @RequestParam(required = false) TicketCategory category,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ticketService.listAllTickets(status, category, page, size));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketDto> getTicket(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicketForAdmin(id));
    }

    @PostMapping("/tickets/{id}/responses")
    public ResponseEntity<TicketDto> respond(@AuthenticationPrincipal String adminId,
                                              @PathVariable UUID id,
                                              @RequestParam(required = false) String userEmail,
                                              @Valid @RequestBody AddResponseRequest request) {
        return ResponseEntity.ok(ticketService.addAdminResponse(id, UUID.fromString(adminId), userEmail, request));
    }

    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<TicketDto> updateStatus(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/tickets/stats/open-count")
    public ResponseEntity<Long> countOpenTickets() {
        return ResponseEntity.ok(ticketService.countOpenTickets());
    }

    @PostMapping("/knowledge")
    public ResponseEntity<ArticleDto> createArticle(@AuthenticationPrincipal String adminId,
                                                     @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(knowledgeService.create(UUID.fromString(adminId), request));
    }

    @PutMapping("/knowledge/{id}")
    public ResponseEntity<ArticleDto> updateArticle(@PathVariable UUID id,
                                                     @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(knowledgeService.update(id, request));
    }

    @DeleteMapping("/knowledge/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable UUID id) {
        knowledgeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
