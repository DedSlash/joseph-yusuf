package com.josephyusuf.support.service;

import com.josephyusuf.support.entity.Ticket;
import com.josephyusuf.support.entity.TicketResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${app.support-url}")
    private String supportUrl;

    public void sendTicketCreatedEmail(String to, Ticket ticket) {
        if (to == null || to.isBlank()) {
            log.warn("Email destinataire vide pour ticket {} — pas d'envoi", ticket.getId());
            return;
        }
        String body = "Bonjour,\n\n"
                + "Votre demande de support a bien été reçue.\n\n"
                + "Sujet : " + ticket.getSubject() + "\n"
                + "Référence : " + ticket.getId() + "\n"
                + "Statut : " + ticket.getStatus() + "\n\n"
                + "Vous pouvez suivre l'avancement et répondre depuis :\n"
                + supportUrl + "/" + ticket.getId() + "\n\n"
                + "Notre équipe vous répondra dans les meilleurs délais.\n\n"
                + "L'équipe Joseph·Yusuf";

        send(to, "Joseph·Yusuf — Ticket reçu : " + ticket.getSubject(), body);
    }

    public void sendAdminResponseEmail(String to, Ticket ticket, TicketResponse response) {
        if (to == null || to.isBlank()) {
            log.warn("Email destinataire vide pour ticket {} — pas d'envoi", ticket.getId());
            return;
        }
        String body = "Bonjour,\n\n"
                + "Notre équipe support a répondu à votre ticket :\n\n"
                + "Sujet : " + ticket.getSubject() + "\n"
                + "Référence : " + ticket.getId() + "\n\n"
                + "Réponse :\n"
                + response.getMessage() + "\n\n"
                + "Consultez le ticket et répondez depuis :\n"
                + supportUrl + "/" + ticket.getId() + "\n\n"
                + "L'équipe Joseph·Yusuf";

        send(to, "Joseph·Yusuf — Nouvelle réponse : " + ticket.getSubject(), body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email support envoyé à {} (sujet : {})", to, subject);
        } catch (Exception e) {
            log.warn("Échec envoi email support à {} : {}", to, e.getMessage());
        }
    }
}
