package com.josephyusuf.support.controller;

import com.josephyusuf.support.dto.*;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.service.KnowledgeService;
import com.josephyusuf.support.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class UserSupportController {

    private final TicketService ticketService;
    private final KnowledgeService knowledgeService;

    @PostMapping("/tickets")
    public ResponseEntity<TicketDto> createTicket(@AuthenticationPrincipal String userId,
                                                   @Valid @RequestBody CreateTicketRequest request) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
        TicketDto created = ticketService.createTicket(UUID.fromString(userId), email, request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/tickets")
    public ResponseEntity<PageResponse<TicketDto>> listMyTickets(@AuthenticationPrincipal String userId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ticketService.listMyTickets(UUID.fromString(userId), page, size));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketDto> getMyTicket(@AuthenticationPrincipal String userId,
                                                  @PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicketForUser(id, UUID.fromString(userId)));
    }

    @PostMapping("/tickets/{id}/responses")
    public ResponseEntity<TicketDto> respondToTicket(@AuthenticationPrincipal String userId,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody AddResponseRequest request) {
        return ResponseEntity.ok(ticketService.addUserResponse(id, UUID.fromString(userId), request));
    }

    @GetMapping("/knowledge/search")
    public ResponseEntity<List<ArticleDto>> searchKnowledge(@RequestParam String q) {
        return ResponseEntity.ok(knowledgeService.search(q));
    }

    @GetMapping("/knowledge/category/{category}")
    public ResponseEntity<List<ArticleDto>> listByCategory(@PathVariable TicketCategory category) {
        return ResponseEntity.ok(knowledgeService.listByCategory(category));
    }

    @GetMapping("/knowledge/public/list")
    public ResponseEntity<List<ArticleDto>> listPublic(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(knowledgeService.listPublic(page, size));
    }

    @GetMapping("/knowledge/public/{id}")
    public ResponseEntity<ArticleDto> getArticle(@PathVariable UUID id) {
        return ResponseEntity.ok(knowledgeService.getAndIncrement(id));
    }
}
