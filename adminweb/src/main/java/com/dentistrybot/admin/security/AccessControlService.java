package com.dentistrybot.admin.security;

import com.dentistrybot.admin.repository.AdminUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * SUPER_ADMIN and ZAV_KAFEDRA can manage every unit ("predmet"). A PROFESSOR can
 * only manage units a ZAV_KAFEDRA has explicitly assigned them (professor_unit_assignments).
 * This is the real enforcement point — SecurityConfig only gates by role, not by
 * resource ownership, so every content controller that writes must call this.
 */
@Service
public class AccessControlService {

    private final AdminUserRepository adminUserRepository;

    public AccessControlService(AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    public boolean isSuperAdminOrZavKafedra(Authentication auth) {
        return hasRole(auth, "SUPER_ADMIN") || hasRole(auth, "ZAV_KAFEDRA");
    }

    public boolean canManageUnit(Authentication auth, int unitId) {
        if (auth == null) return false;
        if (isSuperAdminOrZavKafedra(auth)) return true;
        if (!hasRole(auth, "PROFESSOR")) return false;
        return adminUserRepository.findByUsername(auth.getName())
            .map(u -> adminUserRepository.isUnitAssigned(u.getId(), unitId))
            .orElse(false);
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        String target = "ROLE_" + role;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (target.equals(a.getAuthority())) return true;
        }
        return false;
    }
}
