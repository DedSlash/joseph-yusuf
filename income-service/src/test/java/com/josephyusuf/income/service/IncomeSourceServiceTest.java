package com.josephyusuf.income.service;

import com.josephyusuf.income.client.AuthClient;
import com.josephyusuf.income.client.dto.UpdateProfileRequest;
import com.josephyusuf.income.dto.IncomeMapper;
import com.josephyusuf.income.dto.IncomeSourceDto;
import com.josephyusuf.income.dto.IncomeSourceRequest;
import com.josephyusuf.income.entity.IncomeSource;
import com.josephyusuf.income.entity.IncomeSourceType;
import com.josephyusuf.income.exception.IncomeSourceNotFoundException;
import com.josephyusuf.income.exception.PlanLimitExceededException;
import com.josephyusuf.income.exception.UnauthorizedAccessException;
import com.josephyusuf.income.repository.IncomeEntryRepository;
import com.josephyusuf.income.repository.IncomeSourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeSourceServiceTest {

    @Mock
    private IncomeSourceRepository sourceRepository;

    @Mock
    private IncomeEntryRepository entryRepository;

    @Mock
    private IncomeMapper incomeMapper;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private IncomeSourceService incomeSourceService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SOURCE_ID = UUID.randomUUID();

    @Nested
    @DisplayName("create - creation de source de revenu")
    class CreateTests {

        @Test
        @DisplayName("Plan FREE avec 0 source existante - creation reussie")
        void freePlan_noExistingSources_createsSuccessfully() {
            // Given
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Salaire principal")
                    .type(IncomeSourceType.SALARY)
                    .currency("XOF")
                    .build();

            when(sourceRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(0L);

            IncomeSource savedSource = IncomeSource.builder()
                    .id(SOURCE_ID)
                    .userId(USER_ID)
                    .name("Salaire principal")
                    .type(IncomeSourceType.SALARY)
                    .currency("XOF")
                    .active(true)
                    .build();

            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(savedSource);

            IncomeSourceDto expectedDto = IncomeSourceDto.builder()
                    .id(SOURCE_ID)
                    .name("Salaire principal")
                    .type(IncomeSourceType.SALARY)
                    .currency("XOF")
                    .active(true)
                    .build();

            when(incomeMapper.toSourceDto(savedSource)).thenReturn(expectedDto);

            // When
            IncomeSourceDto result = incomeSourceService.create(USER_ID, "FREE", request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Salaire principal");
            assertThat(result.getType()).isEqualTo(IncomeSourceType.SALARY);
            assertThat(result.getCurrency()).isEqualTo("XOF");
            assertThat(result.isActive()).isTrue();

            verify(sourceRepository).countByUserIdAndActiveTrue(USER_ID);
            verify(sourceRepository).save(any(IncomeSource.class));
        }

        @Test
        @DisplayName("Plan FREE avec 1 source existante - PlanLimitExceededException")
        void freePlan_oneExistingSource_throwsPlanLimitExceededException() {
            // Given
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Freelance")
                    .type(IncomeSourceType.FREELANCE)
                    .build();

            when(sourceRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(1L);

            // When / Then
            assertThatThrownBy(() -> incomeSourceService.create(USER_ID, "FREE", request))
                    .isInstanceOf(PlanLimitExceededException.class)
                    .hasMessageContaining("Plan FREE limité à 1 source");

            verify(sourceRepository).countByUserIdAndActiveTrue(USER_ID);
            verify(sourceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Plan PREMIUM avec plusieurs sources existantes - creation reussie (pas de limite)")
        void premiumPlan_multipleExistingSources_createsSuccessfully() {
            // Given
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Location")
                    .type(IncomeSourceType.RENTAL)
                    .currency("EUR")
                    .build();

            IncomeSource savedSource = IncomeSource.builder()
                    .id(SOURCE_ID)
                    .userId(USER_ID)
                    .name("Location")
                    .type(IncomeSourceType.RENTAL)
                    .currency("EUR")
                    .active(true)
                    .build();

            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(savedSource);

            IncomeSourceDto expectedDto = IncomeSourceDto.builder()
                    .id(SOURCE_ID)
                    .name("Location")
                    .type(IncomeSourceType.RENTAL)
                    .currency("EUR")
                    .active(true)
                    .build();

            when(incomeMapper.toSourceDto(savedSource)).thenReturn(expectedDto);

            // When
            IncomeSourceDto result = incomeSourceService.create(USER_ID, "PREMIUM", request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Location");

            // Verify no limit check was performed for PREMIUM plan
            verify(sourceRepository, never()).countByUserIdAndActiveTrue(any());
            verify(sourceRepository).save(any(IncomeSource.class));
        }

        @Test
        @DisplayName("Premiere source du user - sync user.currency via auth-service")
        void firstSourceEver_syncsUserCurrency() {
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Salaire France")
                    .type(IncomeSourceType.SALARY)
                    .currency("EUR")
                    .build();

            when(sourceRepository.countByUserId(USER_ID)).thenReturn(0L);

            IncomeSource savedSource = IncomeSource.builder()
                    .id(SOURCE_ID).userId(USER_ID).name("Salaire France")
                    .type(IncomeSourceType.SALARY).currency("EUR").active(true).build();
            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(savedSource);
            when(incomeMapper.toSourceDto(savedSource)).thenReturn(
                    IncomeSourceDto.builder().id(SOURCE_ID).currency("EUR").build());

            incomeSourceService.create(USER_ID, "PREMIUM", request);

            ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
            verify(authClient).updateProfile(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Source suivante (countByUserId > 0) - pas de sync user.currency")
        void notFirstSource_doesNotSyncUserCurrency() {
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Mission USA")
                    .type(IncomeSourceType.FREELANCE)
                    .currency("USD")
                    .build();

            when(sourceRepository.countByUserId(USER_ID)).thenReturn(2L);

            IncomeSource savedSource = IncomeSource.builder()
                    .id(SOURCE_ID).userId(USER_ID).currency("USD").active(true).build();
            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(savedSource);
            when(incomeMapper.toSourceDto(savedSource)).thenReturn(
                    IncomeSourceDto.builder().id(SOURCE_ID).currency("USD").build());

            incomeSourceService.create(USER_ID, "PREMIUM", request);

            verify(authClient, never()).updateProfile(any());
        }

        @Test
        @DisplayName("Sync user.currency en echec - creation source reussit quand meme")
        void syncFailure_doesNotBlockCreate() {
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Salaire")
                    .type(IncomeSourceType.SALARY)
                    .currency("EUR")
                    .build();

            when(sourceRepository.countByUserId(USER_ID)).thenReturn(0L);
            IncomeSource savedSource = IncomeSource.builder()
                    .id(SOURCE_ID).userId(USER_ID).currency("EUR").active(true).build();
            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(savedSource);
            when(incomeMapper.toSourceDto(savedSource)).thenReturn(
                    IncomeSourceDto.builder().id(SOURCE_ID).currency("EUR").build());

            doThrow(new RuntimeException("auth-service down"))
                    .when(authClient).updateProfile(any());

            IncomeSourceDto result = incomeSourceService.create(USER_ID, "PREMIUM", request);

            assertThat(result).isNotNull();
            assertThat(result.getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("Currency null par defaut XOF")
        void nullCurrency_defaultsToXOF() {
            // Given
            IncomeSourceRequest request = IncomeSourceRequest.builder()
                    .name("Mobile Money")
                    .type(IncomeSourceType.MOBILE_MONEY)
                    .currency(null)
                    .build();

            when(sourceRepository.countByUserIdAndActiveTrue(USER_ID)).thenReturn(0L);

            IncomeSource savedSource = IncomeSource.builder()
                    .id(SOURCE_ID)
                    .userId(USER_ID)
                    .name("Mobile Money")
                    .type(IncomeSourceType.MOBILE_MONEY)
                    .currency("XOF")
                    .active(true)
                    .build();

            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(savedSource);
            when(incomeMapper.toSourceDto(savedSource)).thenReturn(
                    IncomeSourceDto.builder().id(SOURCE_ID).currency("XOF").build());

            // When
            incomeSourceService.create(USER_ID, "FREE", request);

            // Then: verify the saved entity uses XOF
            ArgumentCaptor<IncomeSource> captor = ArgumentCaptor.forClass(IncomeSource.class);
            verify(sourceRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrency()).isEqualTo("XOF");
        }
    }

    @Nested
    @DisplayName("deactivate - desactivation de source")
    class DeactivateTests {

        @Test
        @DisplayName("Desactivation met active a false")
        void deactivate_setsActiveToFalse() {
            // Given
            IncomeSource source = IncomeSource.builder()
                    .id(SOURCE_ID)
                    .userId(USER_ID)
                    .name("Salaire")
                    .type(IncomeSourceType.SALARY)
                    .currency("XOF")
                    .active(true)
                    .build();

            when(sourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
            when(sourceRepository.save(any(IncomeSource.class))).thenReturn(source);

            // When
            incomeSourceService.deactivate(USER_ID, SOURCE_ID);

            // Then
            ArgumentCaptor<IncomeSource> captor = ArgumentCaptor.forClass(IncomeSource.class);
            verify(sourceRepository).save(captor.capture());
            assertThat(captor.getValue().isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("getAndVerifyOwnership - verification de propriete")
    class OwnershipTests {

        @Test
        @DisplayName("Source existante avec bon userId - retourne la source")
        void existingSource_correctOwner_returnsSource() {
            // Given
            IncomeSource source = IncomeSource.builder()
                    .id(SOURCE_ID)
                    .userId(USER_ID)
                    .name("Freelance")
                    .type(IncomeSourceType.FREELANCE)
                    .currency("XOF")
                    .active(true)
                    .build();

            when(sourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

            // When
            IncomeSource result = incomeSourceService.getAndVerifyOwnership(USER_ID, SOURCE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(SOURCE_ID);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Source existante avec mauvais userId - UnauthorizedAccessException")
        void existingSource_wrongOwner_throwsUnauthorizedAccessException() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            IncomeSource source = IncomeSource.builder()
                    .id(SOURCE_ID)
                    .userId(otherUserId)
                    .name("Freelance")
                    .type(IncomeSourceType.FREELANCE)
                    .currency("XOF")
                    .active(true)
                    .build();

            when(sourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

            // When / Then
            assertThatThrownBy(() -> incomeSourceService.getAndVerifyOwnership(USER_ID, SOURCE_ID))
                    .isInstanceOf(UnauthorizedAccessException.class)
                    .hasMessageContaining("Accès non autorisé");
        }

        @Test
        @DisplayName("Source inexistante - IncomeSourceNotFoundException")
        void nonExistentSource_throwsIncomeSourceNotFoundException() {
            // Given
            when(sourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> incomeSourceService.getAndVerifyOwnership(USER_ID, SOURCE_ID))
                    .isInstanceOf(IncomeSourceNotFoundException.class)
                    .hasMessageContaining("Source de revenu introuvable");
        }
    }
}
