package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.*;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.RefreshToken;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.EmailAlreadyExistsException;
import com.josephyusuf.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TrialService trialService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserDto testUserDto;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Joseph")
                .lastName("Yusuf")
                .plan(Plan.FREE)
                .role(Role.USER)
                .enabled(true)
                .country("SN")
                .currency("XOF")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testUserDto = UserDto.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Joseph")
                .lastName("Yusuf")
                .plan(Plan.FREE)
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("register - nominal case returns AuthResponse with tokens and starts trial")
    void register_nominal() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Joseph")
                .lastName("Yusuf")
                .build();

        // Après startTrial, l'utilisateur est rechargé avec plan PREMIUM_PLUS et inTrial=true
        LocalDateTime trialEnd = LocalDateTime.now().plusDays(7);
        User userAfterTrial = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Joseph")
                .lastName("Yusuf")
                .plan(Plan.PREMIUM_PLUS)
                .role(Role.USER)
                .enabled(true)
                .country("SN")
                .currency("XOF")
                .inTrial(true)
                .trialEndsAt(trialEnd)
                .trialUsed(true)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-value")
                .user(userAfterTrial)
                .expiryDate(Instant.now().plusMillis(604800000))
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userAfterTrial));
        when(jwtService.generateAccessToken(eq(userId), eq("test@example.com"), eq(Plan.PREMIUM_PLUS), eq(Role.USER),
                eq("SN"), eq("XOF"), eq(true), any(LocalDateTime.class))).thenReturn("access-token-value");
        when(refreshTokenService.createRefreshToken(userAfterTrial)).thenReturn(refreshToken);
        when(userMapper.toDto(userAfterTrial)).thenReturn(testUserDto);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.getUser()).isEqualTo(testUserDto);

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(trialService).startTrial(userId);
        verify(refreshTokenService).createRefreshToken(userAfterTrial);
    }

    @Test
    @DisplayName("register - duplicate email throws EmailAlreadyExistsException")
    void register_duplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("Joseph")
                .lastName("Yusuf")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("déjà utilisé");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login - nominal case returns AuthResponse with tokens")
    void login_nominal() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-value")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(604800000))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(eq(userId), eq("test@example.com"), eq(Plan.FREE), eq(Role.USER),
                eq("SN"), eq("XOF"), anyBoolean(), any())).thenReturn("access-token-value");
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(refreshToken);
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.getUser()).isEqualTo(testUserDto);
    }

    @Test
    @DisplayName("login - wrong password throws BadCredentialsException")
    void login_wrongPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides");
    }

    @Test
    @DisplayName("login - user not found throws BadCredentialsException")
    void login_userNotFound() {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Identifiants invalides");
    }

    @Test
    @DisplayName("login - disabled account throws BadCredentialsException")
    void login_disabledAccount() {
        testUser.setEnabled(false);

        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Compte désactivé");
    }

    @Test
    @DisplayName("refresh - nominal case returns new access token")
    void refresh_nominal() {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("valid-refresh-token")
                .user(testUser)
                .expiryDate(Instant.now().plusMillis(604800000))
                .build();

        when(refreshTokenService.verifyRefreshToken("valid-refresh-token")).thenReturn(refreshToken);
        when(jwtService.generateAccessToken(eq(userId), eq("test@example.com"), eq(Plan.FREE), eq(Role.USER),
                eq("SN"), eq("XOF"), anyBoolean(), any())).thenReturn("new-access-token");

        TokenResponse response = authService.refresh(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("logout - deletes refresh token")
    void logout_nominal() {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("token-to-delete")
                .build();

        authService.logout(request);

        verify(refreshTokenService).deleteByToken("token-to-delete");
    }

    @Test
    @DisplayName("getCurrentUser - returns UserDto for valid userId")
    void getCurrentUser_nominal() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        UserDto result = authService.getCurrentUser(userId);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("Joseph");
    }

    @Test
    @DisplayName("getCurrentUser - user not found throws BadCredentialsException")
    void getCurrentUser_notFound() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(unknownId))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Utilisateur non trouvé");
    }
}
