package com.josephyusuf.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josephyusuf.auth.dto.*;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.security.JwtAuthenticationFilter;
import com.josephyusuf.auth.service.AuthService;
import com.josephyusuf.auth.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.josephyusuf.auth.service.PasswordResetService passwordResetService;

    @MockBean
    private com.josephyusuf.auth.service.TrialService trialService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("POST /api/auth/register - 201 Created")
    void register_returnsCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 200 OK")
    void login_returnsOk() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - 200 OK")
    void refresh_returnsOk() throws Exception {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("old-refresh-token")
                .build();

        TokenResponse response = TokenResponse.builder()
                .accessToken("new-access-token")
                .build();

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /api/auth/logout - 204 No Content")
    void logout_returnsNoContent() throws Exception {
        RefreshRequest request = RefreshRequest.builder()
                .refreshToken("refresh-token")
                .build();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshRequest.class));
    }

    @Test
    @DisplayName("GET /api/auth/me - 200 OK with authenticated user")
    void me_returnsCurrentUser() throws Exception {
        UserDto userDto = UserDto.builder()
                .id(USER_ID)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .plan(Plan.PREMIUM)
                .build();

        when(authService.getCurrentUser(USER_ID)).thenReturn(userDto);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                USER_ID.toString(), null, List.of(new SimpleGrantedAuthority("PLAN_PREMIUM")));

        mockMvc.perform(get("/api/auth/me")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }
}
