package com.dentistrybot.admin.api;

import com.dentistrybot.admin.model.AdminUser;
import com.dentistrybot.admin.repository.AdminUserRepository;
import com.dentistrybot.admin.security.AccessControlService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ProfessorAssignmentControllerTest {

    private static final Authentication AUTH = mock(Authentication.class);

    private AdminUser professor(int id, String username) {
        AdminUser u = new AdminUser();
        u.setId(id);
        u.setUsername(username);
        u.setRole(AdminUser.ROLE_PROFESSOR);
        return u;
    }

    @Test
    void listProfessorsForbiddenForProfessor() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(false);

        var response = new ProfessorAssignmentController(repo, accessControl).listProfessors(AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(repo);
    }

    @Test
    void listProfessorsReturnsOnlyProfessorRoleAccounts() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AdminUser zav = new AdminUser();
        zav.setId(1);
        zav.setRole(AdminUser.ROLE_ZAV_KAFEDRA);
        when(repo.findAll()).thenReturn(List.of(zav, professor(2, "prof1")));
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(true);

        var response = new ProfessorAssignmentController(repo, accessControl).listProfessors(AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> body = (List<Map<String, Object>>) response.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0)).containsEntry("id", 2).containsEntry("username", "prof1");
    }

    @Test
    void assignForbiddenForProfessor() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(false);

        var response = new ProfessorAssignmentController(repo, accessControl)
            .assign(Map.of("adminUserId", 2, "unitId", 5), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).assignUnit(anyInt(), anyInt(), any());
    }

    @Test
    void assignRejectsMissingFields() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(true);

        var response = new ProfessorAssignmentController(repo, accessControl).assign(Map.of("adminUserId", 2), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void assignReturnsNotFoundForUnknownUser() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(true);

        var response = new ProfessorAssignmentController(repo, accessControl)
            .assign(Map.of("adminUserId", 2, "unitId", 5), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void assignRejectsNonProfessorTarget() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AdminUser zav = new AdminUser();
        zav.setId(2);
        zav.setRole(AdminUser.ROLE_ZAV_KAFEDRA);
        when(repo.findAll()).thenReturn(List.of(zav));
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(true);

        var response = new ProfessorAssignmentController(repo, accessControl)
            .assign(Map.of("adminUserId", 2, "unitId", 5), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).assignUnit(anyInt(), anyInt(), any());
    }

    @Test
    void assignSucceedsForProfessorTarget() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        Authentication zavAuth = mock(Authentication.class);
        when(zavAuth.getName()).thenReturn("zav1");
        when(repo.findAll()).thenReturn(List.of(professor(2, "prof1")));
        when(repo.findByUsername("zav1")).thenReturn(Optional.empty());
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(zavAuth)).thenReturn(true);

        var response = new ProfessorAssignmentController(repo, accessControl)
            .assign(Map.of("adminUserId", 2, "unitId", 5), zavAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).assignUnit(2, 5, null);
    }

    @Test
    void unassignForbiddenForProfessor() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(false);

        var response = new ProfessorAssignmentController(repo, accessControl).unassign(2, 5, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).unassignUnit(anyInt(), anyInt());
    }

    @Test
    void unassignSucceedsForZavKafedra() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(true);

        var response = new ProfessorAssignmentController(repo, accessControl).unassign(2, 5, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).unassignUnit(2, 5);
    }

    @Test
    void listForbiddenForUnrelatedProfessor() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        Authentication prof3Auth = mock(Authentication.class);
        when(prof3Auth.getName()).thenReturn("prof3");
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(prof3Auth)).thenReturn(false);
        when(repo.findByUsername("prof3")).thenReturn(Optional.of(professor(3, "prof3")));

        // requesting a DIFFERENT professor's (id=2) assignments should be blocked
        var response = new ProfessorAssignmentController(repo, accessControl).list(2, prof3Auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listAllowedForOwnAssignments() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        Authentication prof2Auth = mock(Authentication.class);
        when(prof2Auth.getName()).thenReturn("prof2");
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(prof2Auth)).thenReturn(false);
        when(repo.findByUsername("prof2")).thenReturn(Optional.of(professor(2, "prof2")));
        when(repo.getAssignmentsWithUnitNames(2)).thenReturn(List.of(Map.of("unitId", 5, "name", "F1")));

        var response = new ProfessorAssignmentController(repo, accessControl).list(2, prof2Auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
