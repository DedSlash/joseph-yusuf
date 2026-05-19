package com.josephyusuf.support.service;

import com.josephyusuf.support.dto.AddResponseRequest;
import com.josephyusuf.support.dto.CreateTicketRequest;
import com.josephyusuf.support.dto.TicketDto;
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
import com.josephyusuf.support.mapper.TicketMapperImpl;
import com.josephyusuf.support.repository.TicketRepository;
import com.josephyusuf.support.repository.TicketResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketResponseRepository responseRepository;

    @Spy
    private TicketMapper ticketMapper = new TicketMapperImpl();

    @Mock
    private MailService mailService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TicketService ticketService;

    private UUID userId;
    private UUID ticketId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        ticket = Ticket.builder()
                .id(ticketId)
                .userId(userId)
                .subject("Problème")
                .message("Détails")
                .category(TicketCategory.TECHNICAL)
                .priority(TicketPriority.NORMAL)
                .status(TicketStatus.OPEN)
                .aiHandled(false)
                .build();
    }

    @Test
    void createTicket_persists_sendsEmail_andBroadcastsViaWebSocket() {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .subject("Sujet")
                .message("Message")
                .category(TicketCategory.ACCOUNT)
                .build();
        when(ticketRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(ticketId);
            return t;
        });
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());

        TicketDto dto = ticketService.createTicket(userId, "user@example.com", "PREMIUM", req);

        assertThat(dto.getSubject()).isEqualTo("Sujet");
        assertThat(dto.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(dto.getStatus()).isEqualTo(TicketStatus.OPEN);
        verify(mailService).sendTicketCreatedEmail(eq("user@example.com"), any(Ticket.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/support/new-ticket"), any(Object.class));
    }

    @Test
    void createTicket_freeUserGetsPriorityNormal() {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .subject("S")
                .message("M")
                .category(TicketCategory.INCOME)
                .build();
        when(ticketRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        TicketDto dto = ticketService.createTicket(userId, "u@x.com", "FREE", req);

        assertThat(dto.getPriority()).isEqualTo(TicketPriority.NORMAL);
    }

    @Test
    void createTicket_premiumPlusGetsPriorityUrgent() {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .subject("S")
                .message("M")
                .category(TicketCategory.INCOME)
                .build();
        when(ticketRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        TicketDto dto = ticketService.createTicket(userId, "u@x.com", "PREMIUM_PLUS", req);

        assertThat(dto.getPriority()).isEqualTo(TicketPriority.URGENT);
    }

    @Test
    void createTicket_throwsLimit_whenFreeUserHas2OpenTickets() {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .subject("S")
                .message("M")
                .category(TicketCategory.INCOME)
                .build();
        when(ticketRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(2L);

        assertThatThrownBy(() -> ticketService.createTicket(userId, "u@x.com", "FREE", req))
                .isInstanceOf(TicketLimitExceededException.class);
    }

    @Test
    void createTicket_throwsLimit_whenPremiumUserHas5OpenTickets() {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .subject("S")
                .message("M")
                .category(TicketCategory.INCOME)
                .build();
        when(ticketRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(5L);

        assertThatThrownBy(() -> ticketService.createTicket(userId, "u@x.com", "PREMIUM", req))
                .isInstanceOf(TicketLimitExceededException.class);
    }

    @Test
    void createTicket_premiumPlusHasNoLimit() {
        CreateTicketRequest req = CreateTicketRequest.builder()
                .subject("S")
                .message("M")
                .category(TicketCategory.INCOME)
                .build();
        when(ticketRepository.countByUserIdAndStatusIn(eq(userId), any())).thenReturn(100L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        TicketDto dto = ticketService.createTicket(userId, "u@x.com", "PREMIUM_PLUS", req);

        assertThat(dto).isNotNull();
    }

    @Test
    void getTicketForUser_returnsTicket_whenOwner() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());

        TicketDto dto = ticketService.getTicketForUser(ticketId, userId);

        assertThat(dto.getId()).isEqualTo(ticketId);
    }

    @Test
    void getTicketForUser_throwsAccessDenied_whenNotOwner() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.getTicketForUser(ticketId, UUID.randomUUID()))
                .isInstanceOf(TicketAccessDeniedException.class);
    }

    @Test
    void getTicketForUser_throwsNotFound_whenMissing() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketForUser(ticketId, userId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void getTicketForAdmin_returnsAnyTicket_evenIfNotOwner() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());

        TicketDto dto = ticketService.getTicketForAdmin(ticketId);

        assertThat(dto.getUserId()).isEqualTo(userId);
    }

    @Test
    void addUserResponse_reopensClosedTicket_andSavesResponse() {
        ticket.setStatus(TicketStatus.CLOSED);
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());

        ticketService.addUserResponse(ticketId, userId,
                AddResponseRequest.builder().message("ping").build());

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        verify(responseRepository).save(any(TicketResponse.class));
        verify(ticketRepository).save(ticket);
    }

    @Test
    void addUserResponse_throwsAccessDenied_whenNotOwner() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.addUserResponse(ticketId, UUID.randomUUID(),
                AddResponseRequest.builder().message("x").build()))
                .isInstanceOf(TicketAccessDeniedException.class);
    }

    @Test
    void addAdminResponse_marksInProgress_andSendsEmail() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(responseRepository.save(any(TicketResponse.class))).thenAnswer(inv -> inv.getArgument(0));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());

        ticketService.addAdminResponse(ticketId, UUID.randomUUID(), "user@x.com",
                AddResponseRequest.builder().message("réponse admin").build());

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(mailService).sendAdminResponseEmail(eq("user@x.com"), eq(ticket), any(TicketResponse.class));
    }

    @Test
    void updateStatus_setsClosedAt_whenResolved() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());

        ticketService.updateStatus(ticketId, TicketStatus.RESOLVED);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void updateStatus_throwsNotFound_whenMissing() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.updateStatus(ticketId, TicketStatus.CLOSED))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void listMyTickets_returnsPageResponse() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket), PageRequest.of(0, 10), 1);
        when(ticketRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        var result = ticketService.listMyTickets(userId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listAllTickets_filtersByStatusAndCategory() {
        Page<Ticket> page = new PageImpl<>(List.of(ticket));
        when(ticketRepository.searchTickets(any(), any(), any())).thenReturn(page);
        when(responseRepository.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        var result = ticketService.listAllTickets(TicketStatus.OPEN, TicketCategory.TECHNICAL, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void countOpenTickets_delegatesToRepository() {
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(7L);

        assertThat(ticketService.countOpenTickets()).isEqualTo(7L);
    }
}
