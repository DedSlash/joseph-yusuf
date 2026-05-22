package com.josephyusuf.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${app.reset-url}")
    private String resetUrl;

    public void sendPasswordResetEmail(String to, String token) {
        String link = resetUrl + "?token=" + token;
        String body = "Bonjour,\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe Joseph·Yusuf.\n\n"
                + "Cliquez sur le lien ci-dessous (valide 15 minutes) :\n"
                + link + "\n\n"
                + "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email — votre mot de passe restera inchangé.\n\n"
                + "L'équipe Joseph·Yusuf";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Joseph·Yusuf — Réinitialisation de mot de passe");
            message.setText(body);
            mailSender.send(message);
            log.info("Email de reset envoyé à {}", to);
        } catch (Exception e) {
            // En dev sans serveur mail, on log le lien pour permettre les tests manuels
            log.warn("Échec envoi email à {} : {}. Lien de reset (dev only) : {}",
                    to, e.getMessage(), link);
        }
    }

    public void sendWaitlistConfirmationEmail(String to, String planTier, String promoCode) {
        String body = "Bonjour,\n\n"
                + "Merci de votre intérêt pour Joseph·Yusuf — la plateforme qui applique le Principe de Joseph "
                + "(épargner pendant l'abondance, tenir pendant la disette) à vos revenus.\n\n"
                + "Vous êtes inscrit sur la liste d'attente pour le plan " + planTier + ".\n\n"
                + (promoCode != null && !promoCode.isBlank()
                    ? "✦ Avantage anticipateur : le code promo " + promoCode + " vous est réservé. "
                      + "Il vous accordera une remise dès l'ouverture des paiements.\n\n"
                    : "")
                + "Nous vous notifierons par email dès que les paiements seront disponibles "
                + "(Wave, Orange Money, Free Money et carte bancaire via PayDunya).\n\n"
                + "À très bientôt,\n"
                + "L'équipe Joseph·Yusuf";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Joseph·Yusuf — Inscription confirmée sur la liste d'attente");
            message.setText(body);
            mailSender.send(message);
            log.info("Email waitlist envoyé à {} (plan={})", to, planTier);
        } catch (Exception e) {
            log.warn("Échec envoi email waitlist à {} : {} (plan={})",
                    to, e.getMessage(), planTier);
        }
    }
}
