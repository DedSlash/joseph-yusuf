package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.RenewalReminderEmailRequest;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    /**
     * Email envoyé aux utilisateurs encore dans leur fenêtre de 7 jours initiale
     * à l'ouverture des paiements : leur trial continue normalement jusqu'à sa
     * date d'origine, mais ils peuvent souscrire dès maintenant.
     */
    public void sendPaymentsActivatedTrialActive(User user) {
        String originalEnd = user.getTrialEndsAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Bonne nouvelle — les paiements sont maintenant ouverts sur Joseph·Yusuf.\n\n"
                + "Tu es encore dans ta période d'essai gratuite de 7 jours, qui se termine "
                + "le " + originalEnd + ". Aucune action immédiate n'est nécessaire : "
                + "ton accès PREMIUM+ continue jusqu'à cette date.\n\n"
                + "Quand tu souhaiteras souscrire, tu pourras choisir parmi 4 moyens de paiement :\n"
                + "→ Wave\n"
                + "→ Orange Money\n"
                + "→ Free Money\n"
                + "→ Carte bancaire\n\n"
                + "En tant qu'early adopter, ton coupon EARLY50 te donne -50% à vie sur ton "
                + "abonnement :\n"
                + "→ Premium : 1 495 FCFA/mois au lieu de 2 990\n"
                + "→ Premium+ : 2 995 FCFA/mois au lieu de 5 990\n\n"
                + "Sans souscription avant le " + originalEnd + ", ton compte passera "
                + "automatiquement en FREE.\n\n"
                + "Activer mon abonnement :\n"
                + subscriptionUrl + "\n\n"
                + "À très bientôt,\n"
                + "Rey\n"
                + "Fondateur — Joseph·Yusuf";

        sendEmail(user.getEmail(),
                "🌾 Les paiements sont ouverts — ton coupon t'attend",
                body);
    }

    /**
     * Email envoyé aux utilisateurs dont la fenêtre de 7 jours initiale est
     * dépassée (ils ont profité d'une prolongation pendant que les paiements
     * étaient fermés). Délai de grâce de 24h pour souscrire ou downgrade FREE.
     */
    public void sendPaymentsActivatedGrace24h(User user) {
        String body = "Bonjour " + user.getFirstName() + ",\n\n"
                + "Les paiements viennent d'être ouverts sur Joseph·Yusuf.\n\n"
                + "Tu as bénéficié d'une prolongation gratuite au-delà de tes 7 jours "
                + "d'essai initiaux pendant que les paiements n'étaient pas encore "
                + "disponibles. Maintenant qu'ils le sont, tu as 24h pour souscrire "
                + "à ton abonnement, sans quoi ton compte passera automatiquement en FREE.\n\n"
                + "4 moyens de paiement à ta disposition :\n"
                + "→ Wave\n"
                + "→ Orange Money\n"
                + "→ Free Money\n"
                + "→ Carte bancaire\n\n"
                + "En tant qu'early adopter, ton coupon EARLY50 reste valable et te donne "
                + "-50% à vie :\n"
                + "→ Premium : 1 495 FCFA/mois au lieu de 2 990\n"
                + "→ Premium+ : 2 995 FCFA/mois au lieu de 5 990\n\n"
                + "Activer mon abonnement maintenant :\n"
                + subscriptionUrl + "\n\n"
                + "Tes données restent disponibles même en FREE — seules les fonctionnalités "
                + "avancées seront désactivées si tu ne souscris pas.\n\n"
                + "À très bientôt,\n"
                + "Rey\n"
                + "Fondateur — Joseph·Yusuf";

        sendEmail(user.getEmail(),
                "⏰ 24h pour activer ton abonnement Joseph·Yusuf",
                body);
    }

    /**
     * Envoie l'email de rappel de renouvellement (J-3, J-1) ou d'expiration (J+0).
     * Template choisi sur le champ {@code type} de la requête. Le coupon lifetime
     * (s'il existe) est explicitement mentionné pour rassurer sur le prix.
     */
    public void sendRenewalReminder(User user, RenewalReminderEmailRequest request) {
        String type = request.getType() == null ? "" : request.getType();
        String expirationDate = request.getExpiresAt()
                .atZone(ZoneId.of("Africa/Dakar"))
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH));
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String planLabel = planLabel(request.getPlan());
        String priceLine = priceLine(request.getPlan(), request.getCouponApplied(), request.isCouponLifetime());

        String subject;
        String body;
        switch (type) {
            case "J_MINUS_3" -> {
                subject = "🌾 Ton abonnement Joseph·Yusuf expire dans 3 jours";
                body = "Bonjour " + firstName + ",\n\n"
                        + "Petit rappel : ton abonnement " + planLabel + " expire le "
                        + expirationDate + " (dans 3 jours).\n\n"
                        + "Pour conserver ton accès sans interruption, pense à renouveler "
                        + "avant cette date. PayTech ne débite pas automatiquement — chaque "
                        + "renouvellement passe par la page de paiement.\n\n"
                        + priceLine
                        + "Renouveler mon abonnement :\n"
                        + subscriptionUrl + "\n\n"
                        + "À très bientôt,\n"
                        + "🌾 L'équipe Joseph·Yusuf";
            }
            case "J_MINUS_1" -> {
                subject = "⏰ Plus que 24h avant l'expiration de ton abonnement";
                body = "Bonjour " + firstName + ",\n\n"
                        + "Dernier rappel — ton abonnement " + planLabel + " expire demain ("
                        + expirationDate + ").\n\n"
                        + "Si tu ne renouvelles pas, ton accès aux fonctionnalités avancées "
                        + "sera coupé. Tes données restent disponibles : tu pourras "
                        + "réactiver à tout moment.\n\n"
                        + priceLine
                        + "Renouveler maintenant :\n"
                        + subscriptionUrl + "\n\n"
                        + "À très bientôt,\n"
                        + "🌾 L'équipe Joseph·Yusuf";
            }
            case "EXPIRED" -> {
                subject = "Ton abonnement Joseph·Yusuf a expiré";
                body = "Bonjour " + firstName + ",\n\n"
                        + "Ton abonnement " + planLabel + " a expiré aujourd'hui.\n\n"
                        + "Tes données sont conservées intactes. Quand tu seras prêt, "
                        + "tu peux relancer ton abonnement en un clic.\n\n"
                        + priceLine
                        + "Réactiver mon abonnement :\n"
                        + subscriptionUrl + "\n\n"
                        + "À très bientôt,\n"
                        + "🌾 L'équipe Joseph·Yusuf";
            }
            default -> {
                log.warn("RenewalReminder : type inconnu '{}' pour userId={}, email non envoyé",
                        type, user.getId());
                return;
            }
        }
        sendEmail(user.getEmail(), subject, body);
    }

    private String planLabel(Plan plan) {
        return switch (plan) {
            case PREMIUM -> "Premium";
            case PREMIUM_PLUS -> "Premium+";
            default -> "Premium";
        };
    }

    private String priceLine(Plan plan, String couponCode, boolean couponLifetime) {
        long basePrice = plan == Plan.PREMIUM_PLUS ? 5990L : 2990L;
        if (couponLifetime && couponCode != null && !couponCode.isBlank()) {
            long discounted = basePrice / 2;
            return "Avec ton coupon " + couponCode + " (-50% à vie), tu payes "
                    + discounted + " FCFA au lieu de " + basePrice + " FCFA.\n\n";
        }
        return "Tarif : " + basePrice + " FCFA/mois.\n\n";
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
