package com.dentistrybot.admin.api;

import com.dentistrybot.admin.model.AdminUser;
import com.dentistrybot.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    private AdminUserRepository repo;
    private PasswordEncoder encoder;
    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        repo = mock(AdminUserRepository.class);
        encoder = mock(PasswordEncoder.class);
        controller = new AdminUserController(repo, encoder);
        when(encoder.encode(anyString())).thenReturn("hashed");
    }

    private AdminUser user(int id, String username, String role, String fullName) {
        AdminUser u = new AdminUser();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        u.setFullName(fullName);
        return u;
    }

    @Test
    void list_returnsAllUsers() {
        when(repo.findAll()).thenReturn(List.of(
            user(1, "admin", AdminUser.ROLE_SUPER_ADMIN, "Admin"),
            user(2, "prof",  AdminUser.ROLE_PROFESSOR,   null)
        ));

        List<Map<String, Object>> result = controller.list();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("username", "admin")
                                  .containsEntry("role", AdminUser.ROLE_SUPER_ADMIN);
        assertThat(result.get(1)).containsEntry("fullName", "");
    }

    @Test
    void create_validInput_returnsCreatedUser() {
        when(repo.findByUsername("newuser")).thenReturn(Optional.empty());

        var response = controller.create(Map.of(
            "username", "newuser", "password", "pass123",
            "role", AdminUser.ROLE_PROFESSOR, "fullName", "New User"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).create(any(AdminUser.class));
        verify(encoder).encode("pass123");
    }

    @Test
    void create_missingUsername_returnsBadRequest() {
        var response = controller.create(Map.of("password", "pass123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).create(any());
    }

    @Test
    void create_missingPassword_returnsBadRequest() {
        var response = controller.create(Map.of("username", "someone"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).create(any());
    }

    @Test
    void create_invalidRole_returnsBadRequest() {
        when(repo.findByUsername("x")).thenReturn(Optional.empty());

        var response = controller.create(Map.of(
            "username", "x", "password", "pw", "role", "HACKER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "invalid role");
    }

    @Test
    void create_duplicateUsername_returnsBadRequest() {
        when(repo.findByUsername("admin")).thenReturn(Optional.of(user(1, "admin", AdminUser.ROLE_SUPER_ADMIN, null)));

        var response = controller.create(Map.of("username", "admin", "password", "pw"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "username already exists");
    }

    @Test
    void update_validRole_updatesRoleAndName() {
        var response = controller.update(5, Map.of(
            "role", AdminUser.ROLE_ZAV_KAFEDRA, "fullName", "Zav Kafedra"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateRoleAndName(5, AdminUser.ROLE_ZAV_KAFEDRA, "Zav Kafedra");
    }

    @Test
    void update_invalidRole_returnsBadRequest() {
        var response = controller.update(5, Map.of("role", "UNKNOWN"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).updateRoleAndName(anyInt(), anyString(), any());
    }

    @Test
    void update_passwordOnly_updatesPasswordWithoutRoleChange() {
        var response = controller.update(3, Map.of("password", "newSecret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updatePassword(eq(3), anyString());
        verify(repo, never()).updateRoleAndName(anyInt(), anyString(), any());
    }

    @Test
    void delete_notLastUser_deletesSuccessfully() {
        when(repo.count()).thenReturn(3L);

        var response = controller.delete(2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).delete(2);
    }

    @Test
    void delete_lastUser_returnsBadRequest() {
        when(repo.count()).thenReturn(1L);

        var response = controller.delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).delete(anyInt());
        @SuppressWarnings("unchecked")
        var body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("error");
    }
}
