package com.josephyusuf.alert.service;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.dto.IncomeClassifiedEvent;
import com.josephyusuf.alert.dto.RuleAppliedEvent;
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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

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
            case "ABUNDANCE" -> {
                type = AlertType.ABUNDANCE_DETECTED;
                severity = AlertSeverity.SUCCESS;
                title = "Mois d'abondance détecté";
                message = String.format(
                        "Votre revenu de %d/%d (%s) dépasse votre moyenne de %.1f%%. C'est le moment d'épargner davantage selon le Principe de Joseph.",
                        event.getMonth(), event.getYear(), event.getTotalIncome(), event.getPercentageVsAverage());
            }
            case "LEAN" -> {
                type = AlertType.LEAN_DETECTED;
                severity = AlertSeverity.WARNING;
                title = "Mois de disette détecté";
                message = String.format(
                        "Votre revenu de %d/%d (%s) est inférieur à votre moyenne de %.1f%%. Puisez dans votre épargne d'abondance si nécessaire.",
                        event.getMonth(), event.getYear(), event.getTotalIncome(), Math.abs(event.getPercentageVsAverage()));
            }
            default -> {
                return null;
            }
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
    public Alert createFromRuleApplied(RuleAppliedEvent event) {
        Alert alert = Alert.builder()
                .userId(event.getUserId())
                .type(AlertType.RULE_APPLIED)
                .severity(AlertSeverity.INFO)
                .title("Règle appliquée : " + event.getRule())
                .message(String.format(
                        "La règle %s a été appliquée à un revenu de %s.",
                        event.getRule(), event.getTotalIncome()))
                .read(false)
                .month(event.getMonth())
                .year(event.getYear())
                .build();

        return alertRepository.save(alert);
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
