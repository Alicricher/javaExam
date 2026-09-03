package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final LoginRateLimiter rateLimiter;

    public AuthController(AuthenticationManager authenticationManager, LoginRateLimiter rateLimiter) {
        this.authenticationManager = authenticationManager;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        String ip = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        if (rateLimiter.isLocked(ip)) {
            long retryAfter = rateLimiter.secondsUntilUnlock(ip);
            return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
                .body(Map.of("error", "Too many failed attempts", "retryAfterSeconds", retryAfter));
        }

        String username = body.get("username");
        String password = body.get("password");
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
            rateLimiter.recordSuccess(ip);
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
            return ResponseEntity.ok(Map.of("username", auth.getName()));
        } catch (AuthenticationException e) {
            rateLimiter.recordFailure(ip);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        var authorities = auth.getAuthorities();
        String defaultRole = "PROFESSOR";
        String role = authorities == null ? defaultRole : authorities.stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .findFirst().orElse(defaultRole);
        return ResponseEntity.ok(Map.of("username", auth.getName(), "role", role));
    }
}
