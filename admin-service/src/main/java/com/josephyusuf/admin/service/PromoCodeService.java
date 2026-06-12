package com.josephyusuf.admin.service;

import com.josephyusuf.admin.dto.*;
import com.josephyusuf.admin.entity.PromoCode;
import com.josephyusuf.admin.entity.PromoCodeUsage;
import com.josephyusuf.admin.exception.PromoCodeAlreadyExistsException;
import com.josephyusuf.admin.exception.PromoCodeException;
import com.josephyusuf.admin.exception.PromoCodeNotFoundException;
import com.josephyusuf.admin.mapper.PromoCodeMapper;
import com.josephyusuf.admin.repository.PromoCodeRepository;
import com.josephyusuf.admin.repository.PromoCodeUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final PromoCodeMapper promoCodeMapper;

    @Transactional
    public PromoCodeResponse create(PromoCodeRequest request, UUID adminId) {
        String normalizedCode = request.getCode().toUpperCase();
        if (promoCodeRepository.existsByCode(normalizedCode)) {
            throw new PromoCodeAlreadyExistsException("Code promo déjà existant : " + normalizedCode);
        }

        PromoCode promoCode = PromoCode.builder()
                .code(normalizedCode)
                .description(request.getDescription())
                .discountPercent(request.getDiscountPercent())
                .maxUses(request.getMaxUses())
                .expiresAt(request.getExpiresAt())
                .active(true)
                .lifetime(request.isLifetime())
                .paddleDiscountId(normalizePaddleId(request.getPaddleDiscountId()))
                .createdBy(adminId)
                .build();

        promoCode = promoCodeRepository.save(promoCode);
        log.info("Code promo créé : {} ({}%)", normalizedCode, request.getDiscountPercent());
        return promoCodeMapper.toResponse(promoCode);
    }

    @Transactional(readOnly = true)
    public PageResponse<PromoCodeResponse> list(int page, int size, Boolean activeOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PromoCode> result = activeOnly != null
                ? promoCodeRepository.findAllByActive(activeOnly, pageable)
                : promoCodeRepository.findAll(pageable);

        List<PromoCodeResponse> content = result.getContent().stream()
                .map(promoCodeMapper::toResponse)
                .toList();

        return PageResponse.<PromoCodeResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public PromoCodeResponse toggle(UUID id) {
        PromoCode promoCode = findById(id);
        promoCode.setActive(!promoCode.isActive());
        log.info("Code promo {} → active={}", promoCode.getCode(), promoCode.isActive());
        return promoCodeMapper.toResponse(promoCodeRepository.save(promoCode));
    }

    @Transactional(readOnly = true)
    public PromoCodeStatsResponse stats(UUID id) {
        PromoCode promoCode = findById(id);
        long usages = promoCodeUsageRepository.countByPromoCodeId(id);
        BigDecimal estimatedSavings = BigDecimal.valueOf(usages)
                .multiply(BigDecimal.valueOf(promoCode.getDiscountPercent()))
                .setScale(2, RoundingMode.HALF_UP);

        return PromoCodeStatsResponse.builder()
                .id(promoCode.getId())
                .code(promoCode.getCode())
                .totalUsages(usages)
                .maxUses(promoCode.getMaxUses())
                .estimatedSavings(estimatedSavings)
                .active(promoCode.isActive())
                .build();
    }

    @Transactional(readOnly = true)
    public PromoCodeValidation validate(String code, UUID userId) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        return promoCodeRepository.findByCode(normalizedCode)
                .map(promo -> validateUsage(promo, userId))
                .orElse(invalid("Code promo inconnu"));
    }

    @Transactional(readOnly = true)
    public PromoCodeValidation validatePublic(String code) {
        String normalizedCode = code == null ? "" : code.trim().toUpperCase();
        return promoCodeRepository.findByCode(normalizedCode)
                .map(this::validatePublicUsage)
                .orElse(PromoCodeValidation.builder().valid(false).reason("NOT_FOUND").build());
    }

    @Transactional
    public PromoCodeValidation apply(PromoCodeApplyRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase();
        PromoCode promoCode = promoCodeRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new PromoCodeException("Code promo inconnu : " + normalizedCode));

        PromoCodeValidation validation = validateUsage(promoCode, request.getUserId());
        if (!validation.isValid()) {
            throw new PromoCodeException(validation.getReason());
        }

        boolean alreadyUsed = promoCodeUsageRepository
                .existsByPromoCodeIdAndUserId(promoCode.getId(), request.getUserId());

        if (promoCode.isLifetime() && alreadyUsed) {
            log.info("Code promo lifetime {} réappliqué pour utilisateur {} (transactionId={}) — pas de double comptage",
                    normalizedCode, request.getUserId(), request.getTransactionId());
            return validation;
        }

        promoCode.setUsedCount(promoCode.getUsedCount() + 1);
        promoCodeRepository.save(promoCode);

        promoCodeUsageRepository.save(PromoCodeUsage.builder()
                .promoCodeId(promoCode.getId())
                .userId(request.getUserId())
                .transactionId(request.getTransactionId())
                .build());

        log.info("Code promo {} appliqué par utilisateur {} (transactionId={})",
                normalizedCode, request.getUserId(), request.getTransactionId());

        return validation;
    }

    public long countActive() {
        return promoCodeRepository.findAllByActive(true,
                PageRequest.of(0, 1)).getTotalElements();
    }

    private PromoCodeValidation validateUsage(PromoCode promo, UUID userId) {
        if (!promo.isActive()) {
            return invalid("Code promo désactivé");
        }
        if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(Instant.now())) {
            return invalid("Code promo expiré");
        }
        boolean alreadyUsed = promoCodeUsageRepository.existsByPromoCodeIdAndUserId(promo.getId(), userId);
        // Codes lifetime : réutilisables par le même user à chaque renouvellement.
        // On vérifie quand même maxUses sauf si déjà utilisé (cas du renouvellement).
        if (!alreadyUsed && promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            return invalid("Code promo épuisé");
        }
        if (alreadyUsed && !promo.isLifetime()) {
            return invalid("Code promo déjà utilisé par cet utilisateur");
        }
        return PromoCodeValidation.builder()
                .id(promo.getId())
                .code(promo.getCode())
                .discountPercent(promo.getDiscountPercent())
                .valid(true)
                .lifetime(promo.isLifetime())
                .paddleDiscountId(promo.getPaddleDiscountId())
                .build();
    }

    private PromoCodeValidation validatePublicUsage(PromoCode promo) {
        if (!promo.isActive()) {
            return PromoCodeValidation.builder().valid(false).reason("NOT_FOUND").build();
        }
        if (promo.getExpiresAt() != null && promo.getExpiresAt().isBefore(Instant.now())) {
            return PromoCodeValidation.builder().valid(false).reason("EXPIRED").build();
        }
        if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
            return PromoCodeValidation.builder().valid(false).reason("MAX_USES_REACHED").build();
        }
        return PromoCodeValidation.builder()
                .id(promo.getId())
                .code(promo.getCode())
                .discountPercent(promo.getDiscountPercent())
                .valid(true)
                .lifetime(promo.isLifetime())
                .paddleDiscountId(promo.getPaddleDiscountId())
                .build();
    }

    private String normalizePaddleId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PromoCodeValidation invalid(String reason) {
        return PromoCodeValidation.builder().valid(false).reason(reason).build();
    }

    private PromoCode findById(UUID id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new PromoCodeNotFoundException("Code promo introuvable : " + id));
    }
}
