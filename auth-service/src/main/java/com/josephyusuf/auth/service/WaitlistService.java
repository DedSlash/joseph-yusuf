package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.WaitlistJoinRequest;
import com.josephyusuf.auth.dto.WaitlistJoinResponse;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.WaitlistEntry;
import com.josephyusuf.auth.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final EmailService emailService;

    @Value("${app.waitlist.promo-code:EARLY50}")
    private String reservedPromoCode;

    @Transactional
    public WaitlistJoinResponse join(WaitlistJoinRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Plan plan = request.getPlanTier();

        Optional<WaitlistEntry> existing =
                waitlistRepository.findByEmailAndPlanTier(normalizedEmail, plan);

        if (existing.isPresent()) {
            WaitlistEntry entry = existing.get();
            log.info("Waitlist : email déjà inscrit ({}) pour plan {}", normalizedEmail, plan);
            return WaitlistJoinResponse.builder()
                    .email(entry.getEmail())
                    .planTier(plan.name())
                    .promoCodeReserved(entry.getPromoCodeReserved())
                    .alreadyRegistered(true)
                    .message("Votre adresse est déjà sur la liste d'attente pour ce plan. "
                            + "Vous serez notifié dès l'ouverture des paiements.")
                    .build();
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .email(normalizedEmail)
                .planTier(plan)
                .country(request.getCountry() != null ? request.getCountry() : "SN")
                .currency(request.getCurrency() != null ? request.getCurrency() : "XOF")
                .promoCodeReserved(reservedPromoCode)
                .notified(false)
                .build();

        WaitlistEntry saved = waitlistRepository.save(entry);
        log.info("Waitlist : nouvelle inscription email={} plan={} promo={}",
                saved.getEmail(), saved.getPlanTier(), saved.getPromoCodeReserved());

        emailService.sendWaitlistConfirmationEmail(
                saved.getEmail(), plan.name(), saved.getPromoCodeReserved());

        return WaitlistJoinResponse.builder()
                .email(saved.getEmail())
                .planTier(plan.name())
                .promoCodeReserved(saved.getPromoCodeReserved())
                .alreadyRegistered(false)
                .message("Inscription confirmée. Vous serez notifié dès l'ouverture des paiements.")
                .build();
    }

    @Transactional(readOnly = true)
    public long countByPlan(Plan plan) {
        return waitlistRepository.countByPlanTier(plan);
    }
}
