package com.dentistrybot.admin.api;

import com.dentistrybot.admin.model.AdminUser;
import com.dentistrybot.admin.repository.AdminUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-users")
public class AdminUserController {

    private final AdminUserRepository repo;
    private final PasswordEncoder encoder;

    public AdminUserController(AdminUserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll().stream().map(this::toMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.getOrDefault("role", AdminUser.ROLE_PROFESSOR);
        String fullName = body.get("fullName");

        if (username == null || username.isBlank() || password == null || password.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "username and password required"));
        if (!isValidRole(role))
            return ResponseEntity.badRequest().body(Map.of("error", "invalid role"));
        if (repo.findByUsername(username).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error", "username already exists"));

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        user.setFullName(fullName);
        repo.create(user);
        return ResponseEntity.ok(toMap(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        String fullName = body.get("fullName");
        String newPassword = body.get("password");

        if (role != null && !isValidRole(role))
            return ResponseEntity.badRequest().body(Map.of("error", "invalid role"));

        if (role != null) repo.updateRoleAndName(id, role, fullName);
        if (newPassword != null && !newPassword.isBlank())
            repo.updatePassword(id, encoder.encode(newPassword));

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id) {
        if (repo.count() <= 1)
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete the last admin user"));
        repo.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private boolean isValidRole(String role) {
        return AdminUser.ROLE_SUPER_ADMIN.equals(role)
            || AdminUser.ROLE_ZAV_KAFEDRA.equals(role)
            || AdminUser.ROLE_PROFESSOR.equals(role);
    }

    private Map<String, Object> toMap(AdminUser u) {
        return Map.of(
            "id", u.getId(),
            "username", u.getUsername(),
            "role", u.getRole(),
            "fullName", u.getFullName() != null ? u.getFullName() : "",
            "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }
}
