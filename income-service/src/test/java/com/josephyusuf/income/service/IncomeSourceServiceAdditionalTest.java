package com.josephyusuf.income.service;

import com.josephyusuf.income.dto.IncomeMapper;
import com.josephyusuf.income.dto.IncomeSourceDto;
import com.josephyusuf.income.dto.IncomeSourceRequest;
import com.josephyusuf.income.entity.IncomeSource;
import com.josephyusuf.income.entity.IncomeSourceType;
import com.josephyusuf.income.repository.IncomeSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeSourceServiceAdditionalTest {

    @Mock
    private IncomeSourceRepository sourceRepository;

    @Mock
    private IncomeMapper incomeMapper;

    @InjectMocks
    private IncomeSourceService sourceService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();

    @Test
    @DisplayName("listByUser - returns mapped active sources")
    void listByUser_returnsMappedSources() {
        IncomeSource source = IncomeSource.builder()
                .id(SOURCE_ID).userId(USER_ID).name("Salaire")
                .type(IncomeSourceType.SALARY).currency("XOF").active(true).build();

        when(sourceRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(source));

        IncomeSourceDto dto = IncomeSourceDto.builder()
                .id(SOURCE_ID).name("Salaire").type(IncomeSourceType.SALARY).build();
        when(incomeMapper.toSourceDto(source)).thenReturn(dto);

        List<IncomeSourceDto> result = sourceService.listByUser(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Salaire");
    }

    @Test
    @DisplayName("listByUser - returns empty list when no active sources")
    void listByUser_empty() {
        when(sourceRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of());

        List<IncomeSourceDto> result = sourceService.listByUser(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("update - updates name and type, keeps currency when null in request")
    void update_nullCurrency_keepsCurrent() {
        IncomeSource source = IncomeSource.builder()
                .id(SOURCE_ID).userId(USER_ID).name("Old")
                .type(IncomeSourceType.SALARY).currency("XOF").active(true).build();

        when(sourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(sourceRepository.save(any(IncomeSource.class))).thenReturn(source);

        IncomeSourceDto dto = IncomeSourceDto.builder()
                .id(SOURCE_ID).name("New").type(IncomeSourceType.FREELANCE).currency("XOF").build();
        when(incomeMapper.toSourceDto(any())).thenReturn(dto);

        IncomeSourceRequest request = IncomeSourceRequest.builder()
                .name("New").type(IncomeSourceType.FREELANCE).currency(null).build();

        IncomeSourceDto result = sourceService.update(USER_ID, SOURCE_ID, request);

        assertThat(result.getName()).isEqualTo("New");
        verify(sourceRepository).save(argThat(s -> s.getCurrency().equals("XOF")));
    }

    @Test
    @DisplayName("update - updates currency when provided in request")
    void update_withCurrency_updatesCurrency() {
        IncomeSource source = IncomeSource.builder()
                .id(SOURCE_ID).userId(USER_ID).name("Source")
                .type(IncomeSourceType.SALARY).currency("XOF").active(true).build();

        when(sourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(sourceRepository.save(any(IncomeSource.class))).thenReturn(source);

        IncomeSourceDto dto = IncomeSourceDto.builder()
                .id(SOURCE_ID).name("Source").currency("EUR").build();
        when(incomeMapper.toSourceDto(any())).thenReturn(dto);

        IncomeSourceRequest request = IncomeSourceRequest.builder()
                .name("Source").type(IncomeSourceType.SALARY).currency("EUR").build();

        sourceService.update(USER_ID, SOURCE_ID, request);

        verify(sourceRepository).save(argThat(s -> s.getCurrency().equals("EUR")));
    }
}
