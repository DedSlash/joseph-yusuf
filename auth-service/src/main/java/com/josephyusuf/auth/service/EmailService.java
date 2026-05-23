package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

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

    public void sendTrialWelcome(User user) {
        String expirationDate = user.getTrialEndsAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Bienvenue sur Joseph·Yusuf !\n\n"
                + "Nous vous offrons 7 jours d'accès complet PREMIUM_PLUS pour découvrir toutes les fonctionnalités :\n\n"
                + "✅ Sources de revenus illimitées\n"
                + "✅ Toutes les règles financières\n"
                + "✅ Objectifs d'épargne illimités\n"
                + "✅ Rapports PDF mensuels et annuels\n"
                + "✅ Support prioritaire\n\n"
                + "Votre accès gratuit expire le " + expirationDate + ".\n\n"
                + "Les moyens de paiement seront ouverts très bientôt. Quand ce sera le cas, "
                + "vous pourrez utiliser le code EARLY50 pour bénéficier de -50% à vie — "
                + "réservé aux 100 premiers inscrits.\n\n"
                + "Si vous ne souhaitez pas continuer, aucune action n'est nécessaire — "
                + "votre compte passera automatiquement en FREE le " + expirationDate + ".\n\n"
                + "Bonne découverte !\n"
                + "L'équipe Joseph·Yusuf";

        sendSimpleEmail(user.getEmail(),
                "🌟 Votre accès PREMIUM_PLUS est activé — 7 jours gratuits !",
                body);
    }

    public void sendTrialReminder(User user) {
        String expirationDate = user.getTrialEndsAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Votre période d'essai PREMIUM_PLUS se termine demain le " + expirationDate + ".\n\n"
                + "Sans action de votre part, votre compte passera automatiquement en FREE demain. "
                + "Vous garderez vos données et toutes vos saisies — seules les fonctionnalités "
                + "avancées seront désactivées.\n\n"
                + "Les moyens de paiement seront ouverts très bientôt. Quand ce sera le cas, "
                + "le code EARLY50 vous donnera -50% à vie — réservé aux 100 premiers inscrits.\n\n"
                + "Nous vous préviendrons dès l'ouverture des paiements.\n\n"
                + "L'équipe Joseph·Yusuf";

        sendSimpleEmail(user.getEmail(),
                "⏰ Votre accès PREMIUM_PLUS expire demain",
                body);
    }

    public void sendTrialExpired(User user) {
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Votre période d'essai est terminée.\n"
                + "Votre compte est maintenant en FREE — toutes vos données restent disponibles.\n\n"
                + "Les moyens de paiement ne sont pas encore activés. Dès qu'ils le seront, "
                + "vous pourrez passer en PREMIUM ou PREMIUM_PLUS, et utiliser le code EARLY50 "
                + "(-50% à vie, réservé aux 100 premiers inscrits) s'il est encore disponible.\n\n"
                + "Nous vous préviendrons par email dès l'ouverture des paiements.\n\n"
                + "L'équipe Joseph·Yusuf";

        sendSimpleEmail(user.getEmail(),
                "Votre essai PREMIUM_PLUS est terminé",
                body);
    }

    private void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email envoyé à {} : {}", to, subject);
        } catch (Exception e) {
            log.warn("Échec envoi email à {} : {} (sujet: {})", to, e.getMessage(), subject);
        }
    }
}
