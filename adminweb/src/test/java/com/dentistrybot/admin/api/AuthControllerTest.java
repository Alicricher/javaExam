package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.LoginRateLimiter;
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

    private LoginRateLimiter rateLimiter() {
        return new LoginRateLimiter();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginLocksOutIpAfterFiveFailuresAndReturns429() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");
        AuthController controller = new AuthController(manager, rateLimiter());

        for (int i = 0; i < 5; i++) {
            var response = controller.login(Map.of("username", "admin", "password", "wrong"), request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        var sixth = controller.login(Map.of("username", "admin", "password", "wrong"), request);
        assertThat(sixth.getStatusCode().value()).isEqualTo(429);
        // A locked-out IP must not even reach the authentication manager again.
        verify(manager, times(5)).authenticate(any());
    }

    @Test
    void loginLockoutIsPerIpNotGlobal() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        LoginRateLimiter sharedLimiter = rateLimiter();
        AuthController controller = new AuthController(manager, sharedLimiter);

        HttpServletRequest attacker = mock(HttpServletRequest.class);
        when(attacker.getRemoteAddr()).thenReturn("203.0.113.7");
        for (int i = 0; i < 5; i++) {
            controller.login(Map.of("username", "admin", "password", "wrong"), attacker);
        }
        assertThat(controller.login(Map.of("username", "admin", "password", "wrong"), attacker).getStatusCode().value())
            .isEqualTo(429);

        HttpServletRequest otherUser = mock(HttpServletRequest.class);
        when(otherUser.getRemoteAddr()).thenReturn("198.51.100.20");
        var response = controller.login(Map.of("username", "admin", "password", "wrong"), otherUser);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginSuccessResetsPriorFailureCount() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", "pw");
        when(manager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad"))
            .thenThrow(new BadCredentialsException("bad"))
            .thenThrow(new BadCredentialsException("bad"))
            .thenThrow(new BadCredentialsException("bad"))
            .thenReturn(auth);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");
        when(request.getSession(true)).thenReturn(mock(HttpSession.class));
        AuthController controller = new AuthController(manager, rateLimiter());

        for (int i = 0; i < 4; i++) {
            controller.login(Map.of("username", "admin", "password", "wrong"), request);
        }
        var successResponse = controller.login(Map.of("username", "admin", "password", "pw"), request);
        assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // One more failure after a success should NOT be attempt #5 of the old window — still under the limit.
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        var afterSuccess = controller.login(Map.of("username", "admin", "password", "wrong"), request);
        assertThat(afterSuccess.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginReturnsUnauthorizedOnBadCredentials() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        HttpServletRequest request = mock(HttpServletRequest.class);

        var response = new AuthController(manager, rateLimiter()).login(Map.of("username", "x", "password", "y"), request);

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

        var response = new AuthController(manager, rateLimiter()).login(Map.of("username", "admin", "password", "pw"), request);

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

        var result = new AuthController(manager, rateLimiter()).logout(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(session).invalidate();
    }

    @Test
    void logoutHandlesMissingSessionGracefully() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(false)).thenReturn(null);

        var result = new AuthController(manager, rateLimiter()).logout(request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void meReturnsUnauthorizedWhenNoAuthentication() {
        var response = new AuthController(mock(AuthenticationManager.class), rateLimiter()).me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsUnauthorizedWhenNotAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        var response = new AuthController(mock(AuthenticationManager.class), rateLimiter()).me(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meReturnsUsernameWhenAuthenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin");

        var response = new AuthController(mock(AuthenticationManager.class), rateLimiter()).me(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("username", "admin");
        assertThat(body).containsKey("role");
    }

    @Test
    void meReturnsRoleStrippedOfPrefix() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("prof");
        var authority = mock(org.springframework.security.core.GrantedAuthority.class);
        when(authority.getAuthority()).thenReturn("ROLE_ZAV_KAFEDRA");
        doReturn(java.util.List.of(authority)).when(auth).getAuthorities();

        var response = new AuthController(mock(AuthenticationManager.class), rateLimiter()).me(auth);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("role", "ZAV_KAFEDRA");
    }
}
