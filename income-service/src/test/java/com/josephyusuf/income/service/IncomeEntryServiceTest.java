package com.josephyusuf.income.service;

import com.josephyusuf.income.dto.IncomeEntryDto;
import com.josephyusuf.income.dto.IncomeEntryRequest;
import com.josephyusuf.income.dto.IncomeMapper;
import com.josephyusuf.income.entity.IncomeEntry;
import com.josephyusuf.income.entity.IncomeSource;
import com.josephyusuf.income.entity.IncomeSourceType;
import com.josephyusuf.income.exception.DuplicateEntryException;
import com.josephyusuf.income.exception.IncomeSourceNotFoundException;
import com.josephyusuf.income.exception.UnauthorizedAccessException;
import com.josephyusuf.income.producer.IncomeEventProducer;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeEntryServiceTest {

    @Mock
    private IncomeEntryRepository entryRepository;

    @Mock
    private IncomeSourceService sourceService;

    @Mock
    private IncomeMapper incomeMapper;

    @Mock
    private MonthSummaryService monthSummaryService;

    @Mock
    private IncomeEventProducer eventProducer;

    @Mock
    private CurrencyConverter currencyConverter;

    @InjectMocks
    private IncomeEntryService entryService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("Creation reussie")
        void create_success() {
            IncomeSource source = IncomeSource.builder()
                    .id(SOURCE_ID).userId(USER_ID).name("Salaire")
                    .type(IncomeSourceType.SALARY).currency("XOF").active(true).build();

            IncomeEntryRequest request = IncomeEntryRequest.builder()
                    .incomeSourceId(SOURCE_ID)
                    .amount(new BigDecimal("500000"))
                    .month(5).year(2026).note("Mai").build();

            when(sourceService.getAndVerifyOwnership(USER_ID, SOURCE_ID)).thenReturn(source);
            when(entryRepository.existsByIncomeSourceIdAndMonthAndYear(SOURCE_ID, 5, 2026)).thenReturn(false);
            when(currencyConverter.toXOF(new BigDecimal("500000"), "XOF")).thenReturn(new BigDecimal("500000.00"));

            IncomeEntry savedEntry = IncomeEntry.builder()
                    .id(ENTRY_ID).incomeSource(source).userId(USER_ID)
                    .amount(new BigDecimal("500000")).month(5).year(2026).build();
            when(entryRepository.save(any(IncomeEntry.class))).thenReturn(savedEntry);

            IncomeEntryDto dto = IncomeEntryDto.builder()
                    .id(ENTRY_ID).amount(new BigDecimal("500000")).month(5).year(2026).build();
            when(incomeMapper.toEntryDto(savedEntry)).thenReturn(dto);

            IncomeEntryDto result = entryService.create(USER_ID, request);

            assertThat(result.getId()).isEqualTo(ENTRY_ID);
            assertThat(result.getAmount()).isEqualByComparingTo("500000");
            verify(entryRepository).save(any(IncomeEntry.class));
        }

        @Test
        @DisplayName("Doublon - DuplicateEntryException")
        void create_duplicate_throwsException() {
            IncomeSource source = IncomeSource.builder()
                    .id(SOURCE_ID).userId(USER_ID).build();

            IncomeEntryRequest request = IncomeEntryRequest.builder()
                    .incomeSourceId(SOURCE_ID)
                    .amount(new BigDecimal("500000"))
                    .month(5).year(2026).build();

            when(sourceService.getAndVerifyOwnership(USER_ID, SOURCE_ID)).thenReturn(source);
            when(entryRepository.existsByIncomeSourceIdAndMonthAndYear(SOURCE_ID, 5, 2026)).thenReturn(true);

            assertThatThrownBy(() -> entryService.create(USER_ID, request))
                    .isInstanceOf(DuplicateEntryException.class);

            verify(entryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listByMonthYear")
    class ListTests {

        @Test
        @DisplayName("Retourne la liste mappee")
        void listByMonthYear_returnsMappedList() {
            IncomeEntry entry = IncomeEntry.builder()
                    .id(ENTRY_ID).userId(USER_ID).amount(new BigDecimal("300000")).month(3).year(2026).build();
            when(entryRepository.findByUserIdAndMonthAndYear(USER_ID, 3, 2026))
                    .thenReturn(List.of(entry));

            IncomeEntryDto dto = IncomeEntryDto.builder().id(ENTRY_ID).build();
            when(incomeMapper.toEntryDto(entry)).thenReturn(dto);

            List<IncomeEntryDto> result = entryService.listByMonthYear(USER_ID, 3, 2026);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(ENTRY_ID);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("Mise a jour reussie")
        void update_success() {
            IncomeSource source = IncomeSource.builder()
                    .id(UUID.randomUUID()).currency("XOF").build();
            IncomeEntry entry = IncomeEntry.builder()
                    .id(ENTRY_ID).userId(USER_ID).incomeSource(source)
                    .amount(new BigDecimal("300000")).note("old").build();

            when(entryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
            when(entryRepository.save(any(IncomeEntry.class))).thenReturn(entry);

            IncomeEntryDto dto = IncomeEntryDto.builder()
                    .id(ENTRY_ID).amount(new BigDecimal("400000")).note("updated").build();
            when(incomeMapper.toEntryDto(any())).thenReturn(dto);

            IncomeEntryRequest request = IncomeEntryRequest.builder()
                    .amount(new BigDecimal("400000")).note("updated").build();

            IncomeEntryDto result = entryService.update(USER_ID, ENTRY_ID, request);

            assertThat(result.getNote()).isEqualTo("updated");
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("Suppression reussie")
        void delete_success() {
            IncomeEntry entry = IncomeEntry.builder()
                    .id(ENTRY_ID).userId(USER_ID).build();

            when(entryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            entryService.delete(USER_ID, ENTRY_ID);

            verify(entryRepository).delete(entry);
        }

        @Test
        @DisplayName("Entree inexistante - IncomeSourceNotFoundException")
        void delete_notFound_throwsException() {
            when(entryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> entryService.delete(USER_ID, ENTRY_ID))
                    .isInstanceOf(IncomeSourceNotFoundException.class);
        }

        @Test
        @DisplayName("Mauvais proprietaire - UnauthorizedAccessException")
        void delete_wrongOwner_throwsException() {
            UUID otherUser = UUID.randomUUID();
            IncomeEntry entry = IncomeEntry.builder()
                    .id(ENTRY_ID).userId(otherUser).build();

            when(entryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

            assertThatThrownBy(() -> entryService.delete(USER_ID, ENTRY_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }
}
