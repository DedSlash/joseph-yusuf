package com.josephyusuf.income.controller;

import com.josephyusuf.income.dto.IncomeSourceDto;
import com.josephyusuf.income.dto.IncomeSourceRequest;
import com.josephyusuf.income.entity.IncomeSourceType;
import com.josephyusuf.income.service.IncomeSourceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomeSourceControllerTest {

    @Mock
    private IncomeSourceService sourceService;

    @InjectMocks
    private IncomeSourceController controller;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();

    private UsernamePasswordAuthenticationToken createAuth() {
        return new UsernamePasswordAuthenticationToken(USER_ID.toString(), "PREMIUM", List.of());
    }

    @Test
    @DisplayName("create - returns 201")
    void create_returns201() {
        IncomeSourceRequest request = IncomeSourceRequest.builder()
                .name("Salaire").type(IncomeSourceType.SALARY).build();

        IncomeSourceDto dto = IncomeSourceDto.builder().id(SOURCE_ID).name("Salaire").build();
        when(sourceService.create(eq(USER_ID), eq("PREMIUM"), any())).thenReturn(dto);

        ResponseEntity<IncomeSourceDto> response = controller.create(createAuth(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(SOURCE_ID);
    }

    @Test
    @DisplayName("list - returns 200")
    void list_returns200() {
        IncomeSourceDto dto = IncomeSourceDto.builder().id(SOURCE_ID).build();
        when(sourceService.listByUser(USER_ID)).thenReturn(List.of(dto));

        ResponseEntity<List<IncomeSourceDto>> response = controller.list(createAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("update - returns 200")
    void update_returns200() {
        IncomeSourceRequest request = IncomeSourceRequest.builder()
                .name("Updated").type(IncomeSourceType.FREELANCE).build();

        IncomeSourceDto dto = IncomeSourceDto.builder().id(SOURCE_ID).name("Updated").build();
        when(sourceService.update(eq(USER_ID), eq(SOURCE_ID), any())).thenReturn(dto);

        ResponseEntity<IncomeSourceDto> response = controller.update(createAuth(), SOURCE_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("delete - returns 204")
    void delete_returns204() {
        ResponseEntity<Void> response = controller.delete(createAuth(), SOURCE_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sourceService).deactivate(USER_ID, SOURCE_ID);
    }
}
