package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.reset-url}")
    private String resetUrl;

    @Value("${app.subscription-url:https://josephyusuf.com/subscription}")
    private String subscriptionUrl;

    private InternetAddress getSender() throws Exception {
        String address = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : from;
        return new InternetAddress(address, "Joseph·Yusuf", "UTF-8");
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = resetUrl + "?token=" + token;
        String body = "Bonjour,\n\n"
                + "Vous avez demandé la réinitialisation de votre mot de passe Joseph·Yusuf.\n\n"
                + "Cliquez sur le lien ci-dessous (valide 15 minutes) :\n"
                + link + "\n\n"
                + "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email — votre mot de passe restera inchangé.\n\n"
                + "🌾 L'équipe Joseph·Yusuf";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(getSender());
            helper.setTo(to);
            helper.setSubject("🌾 Joseph·Yusuf — Réinitialisation de mot de passe");
            helper.setText(body);
            mailSender.send(message);
            log.info("Email de reset envoyé à {}", to);
        } catch (Exception e) {
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
                + "🌾 L'équipe Joseph·Yusuf";

        sendEmail(user.getEmail(),
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
                + "🌾 L'équipe Joseph·Yusuf";

        sendEmail(user.getEmail(),
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
                + "🌾 L'équipe Joseph·Yusuf";

        sendEmail(user.getEmail(),
                "Votre essai PREMIUM_PLUS est terminé",
                body);
    }

    public void sendTrialExtended(User user) {
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Ta période d'essai de 7 jours vient de se terminer — et on ne te laisse "
                + "pas sur le carreau.\n\n"
                + "Ton accès Premium+ est prolongé gratuitement jusqu'à l'ouverture officielle "
                + "des paiements sur la plateforme.\n\n"
                + "Pourquoi ? Parce que tu fais partie des tout premiers à avoir fait confiance "
                + "à Joseph·Yusuf. Ce n'est pas anodin pour nous.\n\n"
                + "Quand les paiements seront activés (Wave, Orange Money, carte bancaire), "
                + "tu seras parmi les tout premiers informés — avant tout le monde.\n\n"
                + "Et en tant qu'early adopter, ton coupon EARLY50 (-50% à vie) reste valable "
                + "et réservé pour toi.\n\n"
                + "Continue à explorer l'app librement. Si tu as des retours, des questions, "
                + "ou des idées — réponds directement à cet email. Je lis tout.\n\n"
                + "À très bientôt,\n"
                + "Rey\n"
                + "Fondateur — Joseph·Yusuf\n"
                + "josephyusuf.com";

        sendEmail(user.getEmail(),
                "🌾 Ton accès Joseph·Yusuf continue",
                body);
    }

    public void sendPaymentsActivated(User user) {
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Bonne nouvelle — les paiements sont maintenant disponibles sur Joseph·Yusuf.\n\n"
                + "Tu peux désormais activer ton abonnement Premium ou Premium+ directement "
                + "depuis l'application via Wave, Orange Money ou carte bancaire.\n\n"
                + "En tant qu'early adopter, ton coupon EARLY50 te donne -50% à vie sur ton "
                + "abonnement.\n\n"
                + "→ Premium : 1 500 FCFA/mois au lieu de 3 000\n"
                + "→ Premium+ : 3 000 FCFA/mois au lieu de 6 000\n\n"
                + "Ton accès continue normalement jusqu'à ce que tu choisisses de souscrire — "
                + "aucune interruption.\n\n"
                + "Active ton abonnement ici :\n"
                + subscriptionUrl + "\n\n"
                + "À très bientôt,\n"
                + "Rey\n"
                + "Fondateur — Joseph·Yusuf";

        sendEmail(user.getEmail(),
                "🌾 Les paiements sont ouverts — ton coupon t'attend",
                body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(getSender());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            mailSender.send(message);
            log.info("Email envoyé à {} : {}", to, subject);
        } catch (Exception e) {
            log.warn("Échec envoi email à {} : {} (sujet: {})", to, e.getMessage(), subject);
        }
    }
}
