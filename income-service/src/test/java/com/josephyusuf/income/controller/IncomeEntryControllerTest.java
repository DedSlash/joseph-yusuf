package com.josephyusuf.income.controller;

import com.josephyusuf.income.dto.IncomeEntryDto;
import com.josephyusuf.income.dto.IncomeEntryRequest;
import com.josephyusuf.income.service.IncomeEntryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeEntryControllerTest {

    @Mock
    private IncomeEntryService entryService;

    @InjectMocks
    private IncomeEntryController controller;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ENTRY_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID.toString(), "FREE", List.of());
    }

    @Test
    @DisplayName("create - returns 201")
    void create_returns201() {
        IncomeEntryRequest request = IncomeEntryRequest.builder()
                .incomeSourceId(UUID.randomUUID())
                .amount(new BigDecimal("500000"))
                .month(5).year(2026).build();

        IncomeEntryDto dto = IncomeEntryDto.builder().id(ENTRY_ID).amount(new BigDecimal("500000")).build();
        when(entryService.create(eq(USER_ID), any())).thenReturn(dto);

        ResponseEntity<IncomeEntryDto> response = controller.create(createAuth(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(ENTRY_ID);
    }

    @Test
    @DisplayName("list - returns 200 by month/year")
    void list_returns200() {
        IncomeEntryDto dto = IncomeEntryDto.builder().id(ENTRY_ID).build();
        when(entryService.listByMonthYear(USER_ID, 5, 2026)).thenReturn(List.of(dto));

        ResponseEntity<List<IncomeEntryDto>> response = controller.list(createAuth(), 5, 2026, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("list - returns 200 by sourceId")
    void list_returns200_bySource() {
        UUID sourceId = UUID.randomUUID();
        IncomeEntryDto dto = IncomeEntryDto.builder().id(ENTRY_ID).build();
        when(entryService.listBySource(USER_ID, sourceId)).thenReturn(List.of(dto));

        ResponseEntity<List<IncomeEntryDto>> response = controller.list(createAuth(), null, null, sourceId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("list - returns 400 when no params")
    void list_returns400_whenNoParams() {
        ResponseEntity<List<IncomeEntryDto>> response = controller.list(createAuth(), null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("update - returns 200")
    void update_returns200() {
        IncomeEntryRequest request = IncomeEntryRequest.builder()
                .amount(new BigDecimal("600000")).build();

        IncomeEntryDto dto = IncomeEntryDto.builder().id(ENTRY_ID).amount(new BigDecimal("600000")).build();
        when(entryService.update(eq(USER_ID), eq(ENTRY_ID), any())).thenReturn(dto);

        ResponseEntity<IncomeEntryDto> response = controller.update(createAuth(), ENTRY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("delete - returns 204")
    void delete_returns204() {
        ResponseEntity<Void> response = controller.delete(createAuth(), ENTRY_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(entryService).delete(USER_ID, ENTRY_ID);
    }
}
