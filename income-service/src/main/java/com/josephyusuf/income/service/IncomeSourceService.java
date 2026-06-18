package com.josephyusuf.income.service;

import com.josephyusuf.income.client.AuthClient;
import com.josephyusuf.income.client.dto.UpdateProfileRequest;
import com.josephyusuf.income.dto.*;
import com.josephyusuf.income.entity.IncomeEntry;
import com.josephyusuf.income.entity.IncomeSource;
import com.josephyusuf.income.exception.IncomeSourceNotFoundException;
import com.josephyusuf.income.exception.PlanLimitExceededException;
import com.josephyusuf.income.exception.UnauthorizedAccessException;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import com.josephyusuf.income.repository.IncomeSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeSourceService {

    private final IncomeSourceRepository sourceRepository;
    private final IncomeEntryRepository entryRepository;
    private final IncomeMapper incomeMapper;
    private final AuthClient authClient;

    @Transactional
    public IncomeSourceDto create(UUID userId, String plan, IncomeSourceRequest request) {
        if ("FREE".equals(plan)) {
            long activeCount = sourceRepository.countByUserIdAndActiveTrue(userId);
            if (activeCount >= 1) {
                throw new PlanLimitExceededException(
                        "Plan FREE limité à 1 source de revenu. Passez en Premium.");
            }
        }

        boolean isFirstSourceEver = sourceRepository.countByUserId(userId) == 0;
        String currency = request.getCurrency() != null ? request.getCurrency() : "XOF";

        IncomeSource source = IncomeSource.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType())
                .currency(currency)
                .active(true)
                .build();

        source = sourceRepository.save(source);

        if (isFirstSourceEver) {
            syncUserDisplayCurrency(userId, currency);
        }

        return incomeMapper.toSourceDto(source);
    }

    private void syncUserDisplayCurrency(UUID userId, String currency) {
        try {
            authClient.updateProfile(UpdateProfileRequest.builder().currency(currency).build());
        } catch (Exception e) {
            log.warn("Failed to sync display currency for user {}: {}", userId, e.getMessage());
        }
    }

    public List<IncomeSourceDto> listByUser(UUID userId) {
        return sourceRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(incomeMapper::toSourceDto)
                .toList();
    }

    @Transactional
    public IncomeSourceDto update(UUID userId, UUID sourceId, IncomeSourceRequest request) {
        IncomeSource source = getAndVerifyOwnership(userId, sourceId);
        source.setName(request.getName());
        source.setType(request.getType());
        if (request.getCurrency() != null) {
            source.setCurrency(request.getCurrency());
        }
        source = sourceRepository.save(source);
        return incomeMapper.toSourceDto(source);
    }

    @Transactional
    public void deactivate(UUID userId, UUID sourceId) {
        IncomeSource source = getAndVerifyOwnership(userId, sourceId);
        // Supprimer toutes les entrées liées avant le soft-delete de la source
        List<IncomeEntry> entries = entryRepository.findByIncomeSourceIdAndUserId(sourceId, userId);
        entryRepository.deleteAll(entries);
        source.setActive(false);
        sourceRepository.save(source);
    }

    public IncomeSource getAndVerifyOwnership(UUID userId, UUID sourceId) {
        IncomeSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new IncomeSourceNotFoundException("Source de revenu introuvable"));
        if (!source.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Accès non autorisé à cette ressource");
        }
        return source;
    }
}
