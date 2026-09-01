package com.dentistrybot.admin.security;

import com.dentistrybot.admin.model.AdminUser;
import com.dentistrybot.admin.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the real AccessControlService logic (not mocked) - the controller test
 * files all mock this class away, so it needs its own direct coverage of the role
 * and unit-assignment rules it's supposed to enforce.
 */
class AccessControlServiceTest {

    private Authentication authWithRole(String role) {
        Authentication auth = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_" + role))).when(auth).getAuthorities();
        return auth;
    }

    @Test
    void superAdminCanManageAnyUnit() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = new AccessControlService(repo);

        assertThat(accessControl.canManageUnit(authWithRole(AdminUser.ROLE_SUPER_ADMIN), 999)).isTrue();
    }

    @Test
    void zavKafedraCanManageAnyUnit() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = new AccessControlService(repo);

        assertThat(accessControl.canManageUnit(authWithRole(AdminUser.ROLE_ZAV_KAFEDRA), 999)).isTrue();
    }

    @Test
    void professorCanManageOnlyAssignedUnit() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        Authentication auth = authWithRole(AdminUser.ROLE_PROFESSOR);
        when(auth.getName()).thenReturn("prof1");
        AdminUser prof = new AdminUser();
        prof.setId(42);
        when(repo.findByUsername("prof1")).thenReturn(Optional.of(prof));
        when(repo.isUnitAssigned(42, 5)).thenReturn(true);
        when(repo.isUnitAssigned(42, 6)).thenReturn(false);
        AccessControlService accessControl = new AccessControlService(repo);

        assertThat(accessControl.canManageUnit(auth, 5)).isTrue();
        assertThat(accessControl.canManageUnit(auth, 6)).isFalse();
    }

    @Test
    void professorWithUnknownAccountCannotManageAnything() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        Authentication auth = authWithRole(AdminUser.ROLE_PROFESSOR);
        when(auth.getName()).thenReturn("ghost");
        when(repo.findByUsername("ghost")).thenReturn(Optional.empty());
        AccessControlService accessControl = new AccessControlService(repo);

        assertThat(accessControl.canManageUnit(auth, 5)).isFalse();
    }

    @Test
    void nullAuthenticationCannotManageAnything() {
        AccessControlService accessControl = new AccessControlService(mock(AdminUserRepository.class));

        assertThat(accessControl.canManageUnit(null, 5)).isFalse();
    }

    @Test
    void unrecognizedRoleCannotManageAnything() {
        AdminUserRepository repo = mock(AdminUserRepository.class);
        AccessControlService accessControl = new AccessControlService(repo);

        assertThat(accessControl.canManageUnit(authWithRole("SOMETHING_ELSE"), 5)).isFalse();
    }

    @Test
    void isSuperAdminOrZavKafedraRejectsProfessor() {
        AccessControlService accessControl = new AccessControlService(mock(AdminUserRepository.class));

        assertThat(accessControl.isSuperAdminOrZavKafedra(authWithRole(AdminUser.ROLE_PROFESSOR))).isFalse();
    }
}
