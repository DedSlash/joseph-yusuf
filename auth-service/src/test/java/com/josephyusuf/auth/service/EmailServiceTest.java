package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.RenewalReminderEmailRequest;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    private EmailService emailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "from", "no-reply@josephyusuf.com");
        ReflectionTestUtils.setField(emailService, "mailUsername", "no-reply@josephyusuf.com");
        ReflectionTestUtils.setField(emailService, "resetUrl", "https://josephyusuf.com/reset-password");
        ReflectionTestUtils.setField(emailService, "subscriptionUrl", "https://josephyusuf.com/subscription");
    }

    private User trialUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .firstName("Sira")
                .lastName("Diallo")
                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build();
    }

    private MimeMessage captureSentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private String subject(MimeMessage msg) {
        try { return msg.getSubject(); } catch (Exception e) { return null; }
    }

    private String firstRecipient(MimeMessage msg) {
        try { return msg.getAllRecipients()[0].toString(); } catch (Exception e) { return null; }
    }

    @Test
    @DisplayName("sendPasswordResetEmail : sujet reset + destinataire OK + lien token")
    void sendPasswordResetEmail_nominal() throws Exception {
        emailService.sendPasswordResetEmail("user@example.com", "tok-123");

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("Réinitialisation");
        assertThat(firstRecipient(sent)).isEqualTo("user@example.com");
        assertThat((String) sent.getContent()).contains("https://josephyusuf.com/reset-password?token=tok-123");
    }

    @Test
    @DisplayName("sendTrialWelcome : sujet et destinataire OK")
    void sendTrialWelcome_nominal() {
        emailService.sendTrialWelcome(trialUser());

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("PREMIUM_PLUS");
        assertThat(firstRecipient(sent)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("sendTrialReminder : sujet expire demain")
    void sendTrialReminder_nominal() {
        emailService.sendTrialReminder(trialUser());

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("expire demain");
    }

    @Test
    @DisplayName("sendTrialExpired : sujet essai terminé")
    void sendTrialExpired_nominal() {
        emailService.sendTrialExpired(trialUser());

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("terminé");
    }

    @Test
    @DisplayName("sendTrialExtended : sujet accès continue + signé Rey")
    void sendTrialExtended_nominal() throws Exception {
        emailService.sendTrialExtended(trialUser());

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("continue");
        assertThat((String) sent.getContent()).contains("Rey");
    }

    @Test
    @DisplayName("sendPaymentsActivatedTrialActive : mention coupon EARLY50 + 4 moyens")
    void sendPaymentsActivatedTrialActive_nominal() throws Exception {
        emailService.sendPaymentsActivatedTrialActive(trialUser());

        MimeMessage sent = captureSentMessage();
        assertThat((String) sent.getContent())
                .contains("EARLY50")
                .contains("Wave")
                .contains("Orange Money");
    }

    @Test
    @DisplayName("sendPaymentsActivatedGrace24h : mention 24h + EARLY50")
    void sendPaymentsActivatedGrace24h_nominal() throws Exception {
        emailService.sendPaymentsActivatedGrace24h(trialUser());

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("24h");
        assertThat((String) sent.getContent()).contains("EARLY50");
    }

    @Test
    @DisplayName("sendRenewalReminder J_MINUS_3 : sujet 3 jours + prix avec coupon lifetime")
    void sendRenewalReminder_jMinus3_withLifetimeCoupon() throws Exception {
        RenewalReminderEmailRequest req = RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM)
                .type("J_MINUS_3")
                .expiresAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .couponApplied("EARLY50")
                .couponLifetime(true)
                .build();

        emailService.sendRenewalReminder(trialUser(), req);

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("3 jours");
        assertThat((String) sent.getContent())
                .contains("EARLY50")
                .contains("1495 FCFA")
                .contains("2990 FCFA");
    }

    @Test
    @DisplayName("sendRenewalReminder J_MINUS_3 sans coupon : prix de base")
    void sendRenewalReminder_jMinus3_noCoupon() throws Exception {
        RenewalReminderEmailRequest req = RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM)
                .type("J_MINUS_3")
                .expiresAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .couponLifetime(false)
                .build();

        emailService.sendRenewalReminder(trialUser(), req);

        MimeMessage sent = captureSentMessage();
        assertThat((String) sent.getContent())
                .contains("2990 FCFA/mois")
                .doesNotContain("EARLY50");
    }

    @Test
    @DisplayName("sendRenewalReminder J_MINUS_1 : sujet 24h")
    void sendRenewalReminder_jMinus1_nominal() {
        RenewalReminderEmailRequest req = RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM)
                .type("J_MINUS_1")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();

        emailService.sendRenewalReminder(trialUser(), req);

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("24h");
    }

    @Test
    @DisplayName("sendRenewalReminder EXPIRED : sujet expiré + plan PREMIUM_PLUS price")
    void sendRenewalReminder_expired_premiumPlus() throws Exception {
        RenewalReminderEmailRequest req = RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM_PLUS)
                .type("EXPIRED")
                .expiresAt(Instant.now())
                .build();

        emailService.sendRenewalReminder(trialUser(), req);

        MimeMessage sent = captureSentMessage();
        assertThat(subject(sent)).contains("expiré");
        assertThat((String) sent.getContent()).contains("5990 FCFA/mois");
    }

    @Test
    @DisplayName("sendRenewalReminder PREMIUM_PLUS + coupon lifetime : 2995 FCFA")
    void sendRenewalReminder_premiumPlusWithCoupon() throws Exception {
        RenewalReminderEmailRequest req = RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM_PLUS)
                .type("J_MINUS_3")
                .expiresAt(Instant.now().plus(3, ChronoUnit.DAYS))
                .couponApplied("EARLY50")
                .couponLifetime(true)
                .build();

        emailService.sendRenewalReminder(trialUser(), req);

        MimeMessage sent = captureSentMessage();
        assertThat((String) sent.getContent())
                .contains("2995 FCFA")
                .contains("5990 FCFA");
    }

    @Test
    @DisplayName("sendRenewalReminder type inconnu : aucun mail envoyé")
    void sendRenewalReminder_unknownType_noSend() {
        RenewalReminderEmailRequest req = RenewalReminderEmailRequest.builder()
                .userId(UUID.randomUUID())
                .plan(Plan.PREMIUM)
                .type("WHATEVER")
                .expiresAt(Instant.now())
                .build();

        emailService.sendRenewalReminder(trialUser(), req);

        verify(mailSender, never()).send((MimeMessage) org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("sendEmail : échec SMTP loggé sans lever d'exception")
    void send_failure_swallowed() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send((MimeMessage) org.mockito.ArgumentMatchers.any());

        emailService.sendTrialWelcome(trialUser());
        // pas d'exception remontée → behavior fail-soft
    }

    @Test
    @DisplayName("sendPasswordResetEmail : échec SMTP loggé sans lever d'exception")
    void sendPasswordResetEmail_failureSwallowed() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send((MimeMessage) org.mockito.ArgumentMatchers.any());

        emailService.sendPasswordResetEmail("user@example.com", "tok-fail");
        // pas d'exception remontée
    }

    @Test
    @DisplayName("getSender : fallback sur from si mailUsername blank")
    void getSender_fallbackToFrom() {
        ReflectionTestUtils.setField(emailService, "mailUsername", "");

        emailService.sendTrialWelcome(trialUser());

        verify(mailSender).send((MimeMessage) org.mockito.ArgumentMatchers.any());
    }
}
