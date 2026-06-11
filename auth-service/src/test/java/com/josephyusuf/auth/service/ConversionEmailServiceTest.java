package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionEmailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JavaMailSender mailSender;

    private ConversionEmailService service;

    @BeforeEach
    void setUp() {
        service = new ConversionEmailService(userRepository, mailSender);
        ReflectionTestUtils.setField(service, "from", "no-reply@josephyusuf.com");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://josephyusuf.com");
    }

    private User freeUser(String email, String firstName) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName(firstName)
                .plan(Plan.FREE)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("J+7 + J+30 : envoie un email à chaque user des deux fenêtres")
    void sendConversionEmails_sendsBothCohorts() {
        when(userRepository.findByPlanAndEnabledTrueAndCreatedAtBetween(
                eq(Plan.FREE), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(freeUser("d7@x.com", "Awa")))
                .thenReturn(List.of(freeUser("d30@x.com", "Modou")));

        service.sendConversionEmails();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());

        List<SimpleMailMessage> sent = captor.getAllValues();
        assertThat(sent).hasSize(2);
        assertThat(sent.get(0).getTo()[0]).isEqualTo("d7@x.com");
        assertThat(sent.get(0).getSubject()).contains("premier mois");
        assertThat(sent.get(0).getText()).contains("Awa").contains("PREMIUM");
        assertThat(sent.get(1).getTo()[0]).isEqualTo("d30@x.com");
        assertThat(sent.get(1).getSubject()).contains("Un mois");
        assertThat(sent.get(1).getText()).contains("Modou").contains("3 000 FCFA");
    }

    @Test
    @DisplayName("Aucun user dans les fenêtres : aucun mail envoyé")
    void sendConversionEmails_noCohort_noEmails() {
        when(userRepository.findByPlanAndEnabledTrueAndCreatedAtBetween(
                eq(Plan.FREE), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        service.sendConversionEmails();

        verify(mailSender, never()).send((SimpleMailMessage) any());
    }

    @Test
    @DisplayName("Échec SMTP : log + ne bloque pas le cron")
    void sendConversionEmails_smtpFailure_swallowed() {
        when(userRepository.findByPlanAndEnabledTrueAndCreatedAtBetween(
                eq(Plan.FREE), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(freeUser("d7@x.com", "Awa")))
                .thenReturn(List.of());
        doThrow(new MailSendException("SMTP down")).when(mailSender).send((SimpleMailMessage) any());

        service.sendConversionEmails();
        // pas d'exception remontée
    }
}
