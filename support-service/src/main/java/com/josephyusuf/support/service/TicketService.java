package com.josephyusuf.support.service;

import com.josephyusuf.support.dto.*;
import com.josephyusuf.support.entity.Ticket;
import com.josephyusuf.support.entity.TicketResponse;
import com.josephyusuf.support.enums.ResponderType;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.enums.TicketPriority;
import com.josephyusuf.support.enums.TicketStatus;
import com.josephyusuf.support.exception.TicketAccessDeniedException;
import com.josephyusuf.support.exception.TicketLimitExceededException;
import com.josephyusuf.support.exception.TicketNotFoundException;
import com.josephyusuf.support.mapper.TicketMapper;
import com.josephyusuf.support.repository.TicketRepository;
import com.josephyusuf.support.repository.TicketResponseRepository;
import com.josephyusuf.support.util.PlanRestrictions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketResponseRepository responseRepository;
    private final TicketMapper ticketMapper;
    private final MailService mailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public TicketDto createTicket(UUID userId, String userEmail, String plan, CreateTicketRequest request) {
        List<TicketStatus> openStatuses = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        long openCount = ticketRepository.countByUserIdAndStatusIn(userId, openStatuses);

        int maxTickets = switch (plan) {
            case "PREMIUM_PLUS" -> Integer.MAX_VALUE;
            case "PREMIUM" -> PlanRestrictions.PREMIUM_MAX_OPEN_TICKETS;
            default -> PlanRestrictions.FREE_MAX_OPEN_TICKETS;
        };

        if (openCount >= maxTickets) {
            throw new TicketLimitExceededException(
                    "Limite de tickets atteinte pour votre plan (" + maxTickets + " max). "
                            + "Passez au plan supérieur pour ouvrir plus de tickets.");
        }

        TicketPriority priority = switch (plan) {
            case "PREMIUM_PLUS" -> TicketPriority.URGENT;
            case "PREMIUM" -> TicketPriority.HIGH;
            default -> TicketPriority.NORMAL;
        };

        Ticket ticket = Ticket.builder()
                .userId(userId)
                .subject(request.getSubject())
                .message(request.getMessage())
                .category(request.getCategory())
                .priority(priority)
                .status(TicketStatus.OPEN)
                .aiHandled(false)
                .build();

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} créé par user {}", saved.getId(), userId);

        mailService.sendTicketCreatedEmail(userEmail, saved);
        messagingTemplate.convertAndSend("/topic/support/new-ticket", ticketMapper.toDto(saved));

        return toDtoWithResponses(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketDto> listMyTickets(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> result = ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketDto> listAllTickets(TicketStatus status, TicketCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ticket> result = ticketRepository.searchTickets(status, category, pageable);
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public TicketDto getTicketForUser(UUID ticketId, UUID userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket introuvable : " + ticketId));
        if (!ticket.getUserId().equals(userId)) {
            throw new TicketAccessDeniedException("Ce ticket ne vous appartient pas");
        }
        return toDtoWithResponses(ticket);
    }

    @Transactional(readOnly = true)
    public TicketDto getTicketForAdmin(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket introuvable : " + ticketId));
        return toDtoWithResponses(ticket);
    }

    @Transactional
    public TicketDto addUserResponse(UUID ticketId, UUID userId, AddResponseRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket introuvable : " + ticketId));
        if (!ticket.getUserId().equals(userId)) {
            throw new TicketAccessDeniedException("Ce ticket ne vous appartient pas");
        }

        TicketResponse response = TicketResponse.builder()
                .ticketId(ticketId)
                .responderId(userId)
                .responderType(ResponderType.USER)
                .message(request.getMessage())
                .build();
        responseRepository.save(response);

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        messagingTemplate.convertAndSend("/topic/support/ticket-update", ticket.getId().toString());
        return toDtoWithResponses(ticket);
    }

    @Transactional
    public TicketDto addAdminResponse(UUID ticketId, UUID adminId, String userEmail, AddResponseRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket introuvable : " + ticketId));

        TicketResponse response = TicketResponse.builder()
                .ticketId(ticketId)
                .responderId(adminId)
                .responderType(ResponderType.ADMIN)
                .message(request.getMessage())
                .build();
        TicketResponse savedResponse = responseRepository.save(response);

        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        mailService.sendAdminResponseEmail(userEmail, ticket, savedResponse);
        return toDtoWithResponses(ticket);
    }

    @Transactional
    public TicketDto updateStatus(UUID ticketId, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket introuvable : " + ticketId));
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.RESOLVED) {
            ticket.setClosedAt(Instant.now());
        }
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);
        return toDtoWithResponses(ticket);
    }

    @Transactional(readOnly = true)
    public long countOpenTickets() {
        return ticketRepository.countByStatus(TicketStatus.OPEN);
    }

    private TicketDto toDtoWithResponses(Ticket ticket) {
        TicketDto dto = ticketMapper.toDto(ticket);
        List<TicketResponse> responses = responseRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());
        dto.setResponses(ticketMapper.toResponseDtoList(responses));
        return dto;
    }

    private PageResponse<TicketDto> toPage(Page<Ticket> page) {
        List<TicketDto> content = page.getContent().stream()
                .map(this::toDtoWithResponses)
                .toList();
        return PageResponse.<TicketDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
