package com.josephyusuf.support.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAfter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthHeader_skipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void nonBearerHeader_skipsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic xyz");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void invalidToken_doesNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void validToken_setsAuthenticationWithPlanAndRole() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractUserId("good-token")).thenReturn(userId);
        when(jwtService.extractPlan("good-token")).thenReturn("PREMIUM");
        when(jwtService.extractRole("good-token")).thenReturn("ADMIN");
        when(jwtService.extractEmail("good-token")).thenReturn("a@b.com");

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId);
        assertThat(auth.getCredentials()).isEqualTo("a@b.com");
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("PLAN_PREMIUM", "ROLE_ADMIN");
        verify(chain).doFilter(request, response);
    }

    @Test
    void validToken_withNullPlanAndRole_skipsThoseAuthorities() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer ok");
        when(jwtService.isTokenValid("ok")).thenReturn(true);
        when(jwtService.extractUserId("ok")).thenReturn("u");
        when(jwtService.extractPlan("ok")).thenReturn(null);
        when(jwtService.extractRole("ok")).thenReturn(null);
        when(jwtService.extractEmail("ok")).thenReturn(null);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
