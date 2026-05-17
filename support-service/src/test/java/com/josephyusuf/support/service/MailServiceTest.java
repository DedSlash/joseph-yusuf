package com.josephyusuf.support.service;

import com.josephyusuf.support.entity.Ticket;
import com.josephyusuf.support.entity.TicketResponse;
import com.josephyusuf.support.enums.ResponderType;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.enums.TicketPriority;
import com.josephyusuf.support.enums.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private MailService mailService;

    private Ticket ticket;
    private TicketResponse response;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "from", "no-reply@test.com");
        ReflectionTestUtils.setField(mailService, "supportUrl", "http://localhost:4200/support");

        ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .subject("Test")
                .status(TicketStatus.OPEN)
                .category(TicketCategory.TECHNICAL)
                .priority(TicketPriority.NORMAL)
                .build();
        response = TicketResponse.builder()
                .id(UUID.randomUUID())
                .ticketId(ticket.getId())
                .responderId(UUID.randomUUID())
                .responderType(ResponderType.ADMIN)
                .message("Réponse")
                .build();
    }

    @Test
    void sendTicketCreatedEmail_sendsMessage_whenEmailPresent() {
        mailService.sendTicketCreatedEmail("user@test.com", ticket);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendTicketCreatedEmail_skips_whenEmailBlank() {
        mailService.sendTicketCreatedEmail("  ", ticket);
        mailService.sendTicketCreatedEmail(null, ticket);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendTicketCreatedEmail_swallowsMailException() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        mailService.sendTicketCreatedEmail("user@test.com", ticket);
    }

    @Test
    void sendAdminResponseEmail_sendsMessage() {
        mailService.sendAdminResponseEmail("user@test.com", ticket, response);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAdminResponseEmail_skips_whenEmailNull() {
        mailService.sendAdminResponseEmail(null, ticket, response);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
