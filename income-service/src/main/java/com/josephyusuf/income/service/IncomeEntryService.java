package com.josephyusuf.income.service;

import com.josephyusuf.income.dto.*;
import com.josephyusuf.income.entity.IncomeEntry;
import com.josephyusuf.income.entity.IncomeSource;
import com.josephyusuf.income.exception.DuplicateEntryException;
import com.josephyusuf.income.exception.IncomeSourceNotFoundException;
import com.josephyusuf.income.exception.UnauthorizedAccessException;
import com.josephyusuf.income.producer.IncomeEventProducer;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncomeEntryService {

    private final IncomeEntryRepository entryRepository;
    private final IncomeSourceService sourceService;
    private final MonthSummaryService monthSummaryService;
    private final IncomeEventProducer eventProducer;
    private final IncomeMapper incomeMapper;

    @Transactional
    public IncomeEntryDto create(UUID userId, IncomeEntryRequest request) {
        IncomeSource source = sourceService.getAndVerifyOwnership(userId, request.getIncomeSourceId());

        if (entryRepository.existsByIncomeSourceIdAndMonthAndYear(
                source.getId(), request.getMonth(), request.getYear())) {
            throw new DuplicateEntryException(
                    "Une saisie existe déjà pour cette source en " + request.getMonth() + "/" + request.getYear());
        }

        IncomeEntry entry = IncomeEntry.builder()
                .incomeSource(source)
                .userId(userId)
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .note(request.getNote())
                .build();

        entry = entryRepository.save(entry);

        MonthSummary summary = monthSummaryService.getSummary(userId, request.getMonth(), request.getYear());
        eventProducer.publishIncomeClassified(summary);

        return incomeMapper.toEntryDto(entry);
    }

    public List<IncomeEntryDto> listByMonthYear(UUID userId, int month, int year) {
        return entryRepository.findByUserIdAndMonthAndYear(userId, month, year).stream()
                .map(incomeMapper::toEntryDto)
                .toList();
    }

    @Transactional
    public IncomeEntryDto update(UUID userId, UUID entryId, IncomeEntryRequest request) {
        IncomeEntry entry = getAndVerifyOwnership(userId, entryId);
        entry.setAmount(request.getAmount());
        entry.setNote(request.getNote());
        entry = entryRepository.save(entry);
        return incomeMapper.toEntryDto(entry);
    }

    @Transactional
    public void delete(UUID userId, UUID entryId) {
        IncomeEntry entry = getAndVerifyOwnership(userId, entryId);
        entryRepository.delete(entry);
    }

    private IncomeEntry getAndVerifyOwnership(UUID userId, UUID entryId) {
        IncomeEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new IncomeSourceNotFoundException("Entrée de revenu introuvable"));
        if (!entry.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Accès non autorisé à cette ressource");
        }
        return entry;
    }
}
