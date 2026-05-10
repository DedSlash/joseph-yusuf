package com.josephyusuf.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() { SecurityContextHolder.clearContext(); }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    @DisplayName("No Authorization header → no auth set")
    void noAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Non-Bearer header → no auth")
    void nonBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic xxx");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Valid token with role+plan → sets auth with PLAN_ and ROLE_ authorities")
    void validTokenWithRoleAndPlan() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractUserId("t")).thenReturn("uid");
        when(jwtService.extractPlan("t")).thenReturn("PREMIUM");
        when(jwtService.extractRole("t")).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("uid");
        assertThat(auth.getAuthorities()).extracting("authority")
                .contains("PLAN_PREMIUM", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Valid token without plan/role → empty authorities")
    void validTokenWithoutClaims() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractUserId("t")).thenReturn("uid");
        when(jwtService.extractPlan("t")).thenReturn(null);
        when(jwtService.extractRole("t")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Invalid token → no auth")
    void invalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer t");
        when(jwtService.isTokenValid("t")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
