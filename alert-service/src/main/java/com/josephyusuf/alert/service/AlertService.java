package com.josephyusuf.alert.service;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.dto.IncomeClassifiedEvent;
import com.josephyusuf.alert.dto.RuleAppliedEvent;
import com.josephyusuf.alert.dto.SavingsRecommendationEvent;
import com.josephyusuf.alert.entity.Alert;
import com.josephyusuf.alert.entity.AlertSeverity;
import com.josephyusuf.alert.entity.AlertType;
import com.josephyusuf.alert.exception.AlertNotFoundException;
import com.josephyusuf.alert.exception.UnauthorizedAccessException;
import com.josephyusuf.alert.mapper.AlertMapper;
import com.josephyusuf.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final String STATUS_ABUNDANCE = "ABUNDANCE";
    private static final String STATUS_LEAN = "LEAN";
    private static final String STATUS_NORMAL = "NORMAL";

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    public List<AlertDto> listByUser(UUID userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(alertMapper::toDto)
                .toList();
    }

    public List<AlertDto> listUnread(UUID userId) {
        return alertRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(alertMapper::toDto)
                .toList();
    }

    public long countUnread(UUID userId) {
        return alertRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public AlertDto markAsRead(UUID userId, UUID alertId) {
        Alert alert = getAndVerifyOwnership(userId, alertId);
        alert.setRead(true);
        alert = alertRepository.save(alert);
        return alertMapper.toDto(alert);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Alert> unread = alertRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(a -> a.setRead(true));
        alertRepository.saveAll(unread);
    }

    @Transactional
    public void delete(UUID userId, UUID alertId) {
        Alert alert = getAndVerifyOwnership(userId, alertId);
        alertRepository.delete(alert);
    }

    @Transactional
    public Alert createFromIncomeClassified(IncomeClassifiedEvent event) {
        AlertType type;
        AlertSeverity severity;
        String title;
        String message;

        switch (event.getStatus()) {
            case STATUS_ABUNDANCE -> {
                type = AlertType.ABUNDANCE_DETECTED;
                severity = AlertSeverity.SUCCESS;
                title = "Mois d'abondance détecté";
                message = String.format(
                        "Votre revenu de %s (%s) dépasse votre moyenne de %.1f%%. C'est le moment d'épargner davantage selon le Principe de Joseph.",
                        formatMonthYear(event.getMonth(), event.getYear()), event.getTotalIncome(), event.getPercentageVsAverage());
            }
            case STATUS_LEAN -> {
                type = AlertType.LEAN_DETECTED;
                severity = AlertSeverity.WARNING;
                title = "Mois de disette détecté";
                message = String.format(
                        "Votre revenu de %s (%s) est inférieur à votre moyenne de %.1f%%. Puisez dans votre épargne d'abondance si nécessaire.",
                        formatMonthYear(event.getMonth(), event.getYear()), event.getTotalIncome(), Math.abs(event.getPercentageVsAverage()));
            }
            default -> {
                return null;
            }
        }

        // Une seule alerte ABUNDANCE/LEAN par utilisateur par mois
        if (alertRepository.findByUserIdAndTypeAndMonthAndYear(
                event.getUserId(), type, event.getMonth(), event.getYear()).isPresent()) {
            return null;
        }

        Alert alert = Alert.builder()
                .userId(event.getUserId())
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .read(false)
                .month(event.getMonth())
                .year(event.getYear())
                .build();

        return alertRepository.save(alert);
    }

    @Transactional
    public Alert createFromSavingsRecommendation(SavingsRecommendationEvent event) {
        if (alertRepository.findByUserIdAndTypeAndMonthAndYear(
                event.getUserId(), AlertType.SAVINGS_RECOMMENDATION, event.getMonth(), event.getYear()).isPresent()) {
            return null;
        }

        AlertSeverity severity = switch (event.getJosephStatus() == null ? "" : event.getJosephStatus()) {
            case STATUS_ABUNDANCE -> AlertSeverity.SUCCESS;
            case STATUS_LEAN      -> AlertSeverity.WARNING;
            default               -> AlertSeverity.INFO;
        };

        String title = String.format("Recommandation d'épargne — %s",
                event.getGoalName() != null ? event.getGoalName() : "objectif");
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            message = String.format("Versement recommandé : %s sur %s pour %s.",
                    formatAmount(event.getRecommendedAmount()),
                    event.getGoalName() != null ? event.getGoalName() : "votre objectif",
                    formatMonthYear(event.getMonth(), event.getYear()));
        }

        Alert alert = Alert.builder()
                .userId(event.getUserId())
                .type(AlertType.SAVINGS_RECOMMENDATION)
                .severity(severity)
                .title(title)
                .message(truncate(message, 500))
                .read(false)
                .month(event.getMonth())
                .year(event.getYear())
                .build();

        return alertRepository.save(alert);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    @Transactional
    public Alert createFromRuleApplied(RuleAppliedEvent event) {
        // Une seule alerte RULE_APPLIED par utilisateur par mois
        if (alertRepository.findByUserIdAndTypeAndMonthAndYear(
                event.getUserId(), AlertType.RULE_APPLIED, event.getMonth(), event.getYear()).isPresent()) {
            return null;
        }

        String ruleLabel = toRuleLabel(event.getRule());
        String ruleArticle = toRuleArticle(event.getRule());
        String statusLabel = toStatusLabel(event.getMonthStatus());
        String formattedAmount = formatAmount(event.getTotalIncome());

        Alert alert = Alert.builder()
                .userId(event.getUserId())
                .type(AlertType.RULE_APPLIED)
                .severity(AlertSeverity.INFO)
                .title("Répartition calculée — " + ruleLabel)
                .message(String.format(
                        "Votre répartition de %s a été calculée selon %s pour un revenu de %s%s.",
                        formatMonthYear(event.getMonth(), event.getYear()),
                        ruleArticle + ruleLabel, formattedAmount,
                        statusLabel != null ? " (mois " + statusLabel + ")" : ""))
                .read(false)
                .month(event.getMonth())
                .year(event.getYear())
                .build();

        return alertRepository.save(alert);
    }

    private String toRuleLabel(String rule) {
        if (rule == null) return "règle personnalisée";
        return switch (rule) {
            case "RULE_50_30_20" -> "règle 50/30/20";
            case "RULE_80_20"    -> "règle 80/20 (Pareto)";
            case "RULE_70_20_10" -> "règle 70/20/10";
            case "RULE_JOSEPH"   -> "Principe de Joseph";
            default              -> "règle personnalisée";
        };
    }

    private String toRuleArticle(String rule) {
        if (rule == null) return "la ";
        return switch (rule) {
            case "RULE_JOSEPH" -> "le ";
            default            -> "la ";
        };
    }

    private String toStatusLabel(String status) {
        if (status == null) return null;
        return switch (status) {
            case STATUS_ABUNDANCE -> "d'abondance";
            case STATUS_LEAN      -> "de disette";
            case STATUS_NORMAL    -> null;
            default               -> null;
        };
    }

    private String formatMonthYear(int month, int year) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.FRENCH);
        return monthName.substring(0, 1).toUpperCase() + monthName.substring(1) + " " + year;
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0 XOF";
        return String.format("%,.0f XOF", amount).replace(",", " ");
    }

    private Alert getAndVerifyOwnership(UUID userId, UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alerte introuvable"));
        if (!alert.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Accès non autorisé à cette ressource");
        }
        return alert;
    }
}
