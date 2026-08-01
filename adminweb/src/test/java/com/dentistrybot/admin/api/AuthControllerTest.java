package com.dentistrybot.admin.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginReturnsUnauthorizedOnBadCredentials() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        HttpServletRequest request = mock(HttpServletRequest.class);

        var response = new AuthController(manager).login(Map.of("username", "x", "password", "y"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(request, never()).getSession(anyBoolean());
    }

    @Test
    void loginStoresSecurityContextInSessionOnSuccess() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "pw");
        when(manager.authenticate(any())).thenReturn(auth);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);

        var response = new AuthController(manager).login(Map.of("username", "admin", "password", "pw"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("username", "admin");
        verify(session).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any());
    }

    @Test
    void logoutInvalidatesExistingSession() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        var result = new AuthController(manager).logout(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(session).invalidate();
    }

    @Test
    void logoutHandlesMissingSessionGracefully() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(null);

        var result = new AuthController(manager).logout(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void meReturnsUnauthorizedWhenNoAuthentication() {
        var response = new AuthController(mock(AuthenticationManager.class)).me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsUnauthorizedWhenNotAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        var response = new AuthController(mock(AuthenticationManager.class)).me(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsUsernameWhenAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin");

        var response = new AuthController(mock(AuthenticationManager.class)).me(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("username", "admin");
    }
}
