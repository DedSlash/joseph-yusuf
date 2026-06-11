package com.josephyusuf.auth.service;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.conversion-emails.enabled", havingValue = "true", matchIfMissing = false)
public class ConversionEmailService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Scheduled(cron = "0 0 9 * * *")
    public void sendConversionEmails() {
        log.info("Lancement des emails de conversion...");
        sendDay7Emails();
        sendDay30Emails();
    }

    private void sendDay7Emails() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant eightDaysAgo = Instant.now().minus(8, ChronoUnit.DAYS);

        List<User> users = userRepository.findByPlanAndEnabledTrueAndCreatedAtBetween(
                Plan.FREE, eightDaysAgo, sevenDaysAgo);

        for (User user : users) {
            sendEmail(user.getEmail(),
                    "Vous avez analysé votre premier mois avec Joseph·Yusuf",
                    buildDay7Body(user.getFirstName()));
        }
        if (!users.isEmpty()) {
            log.info("{} emails J+7 envoyés", users.size());
        }
    }

    private void sendDay30Emails() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant thirtyOneDaysAgo = Instant.now().minus(31, ChronoUnit.DAYS);

        List<User> users = userRepository.findByPlanAndEnabledTrueAndCreatedAtBetween(
                Plan.FREE, thirtyOneDaysAgo, thirtyDaysAgo);

        for (User user : users) {
            sendEmail(user.getEmail(),
                    "Un mois avec Joseph·Yusuf — voici ce que PREMIUM vous apporterait",
                    buildDay30Body(user.getFirstName()));
        }
        if (!users.isEmpty()) {
            log.info("{} emails J+30 envoyés", users.size());
        }
    }

    private String buildDay7Body(String firstName) {
        return "Bonjour " + firstName + ",\n\n"
                + "Cela fait une semaine que vous avez rejoint Joseph·Yusuf !\n\n"
                + "Saviez-vous qu'avec le plan PREMIUM (4,99 €/mois), vous pourriez :\n\n"
                + "• Accéder aux 4 règles de répartition (dont la Règle Joseph adaptative)\n"
                + "• Créer jusqu'à 5 objectifs d'épargne avec recommandations personnalisées\n"
                + "• Importer votre historique (Excel, CSV, JSON) pour une analyse immédiate\n"
                + "• Générer des rapports PDF mensuels et annuels\n\n"
                + "Le Principe de Joseph — épargner pendant l'abondance, tenir pendant la disette — "
                + "prend toute sa puissance avec les outils PREMIUM.\n\n"
                + "Passez à PREMIUM : " + frontendUrl + "/subscription\n\n"
                + "L'équipe Joseph·Yusuf";
    }

    private String buildDay30Body(String firstName) {
        return "Bonjour " + firstName + ",\n\n"
                + "Cela fait un mois que vous utilisez Joseph·Yusuf. "
                + "Vous avez déjà franchi le premier pas vers une meilleure gestion de vos revenus.\n\n"
                + "Voici ce que PREMIUM vous apporterait concrètement :\n\n"
                + "• Sources de revenus illimitées (au lieu d'une seule)\n"
                + "• La Règle Joseph : répartition adaptative qui s'ajuste à vos mois d'abondance et de disette\n"
                + "• Jusqu'à 5 objectifs d'épargne avec recommandations au prorata\n"
                + "• Rapports PDF pour suivre votre évolution mois après mois\n"
                + "• Support prioritaire (HIGH priority) avec jusqu'à 5 tickets ouverts\n\n"
                + "Tout cela pour 4,99 €/mois (ou 3 000 FCFA).\n\n"
                + "Passez à PREMIUM : " + frontendUrl + "/subscription\n\n"
                + "L'équipe Joseph·Yusuf";
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("Joseph·Yusuf — " + subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Échec envoi email conversion à {} : {}", to, e.getMessage());
        }
    }
}
