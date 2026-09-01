package com.dentistrybot.admin.api;

import com.dentistrybot.admin.model.AdminUser;
import com.dentistrybot.admin.repository.AdminUserRepository;
import com.dentistrybot.admin.security.AccessControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lets a ZAV_KAFEDRA (or SUPER_ADMIN) assign PROFESSOR accounts to the units
 * ("predmet"/subjects) they're allowed to manage. Deliberately kept off
 * /api/admin-users/** — that prefix is SUPER_ADMIN-only in SecurityConfig, but
 * ZAV_KAFEDRA must be able to do this too.
 */
@RestController
@RequestMapping("/api/professor-assignments")
public class ProfessorAssignmentController {

    private final AdminUserRepository adminUserRepository;
    private final AccessControlService accessControl;

    public ProfessorAssignmentController(AdminUserRepository adminUserRepository, AccessControlService accessControl) {
        this.adminUserRepository = adminUserRepository;
        this.accessControl = accessControl;
    }

    /** Listing PROFESSOR accounts is its own endpoint, off /api/admin-users/** (SUPER_ADMIN-only),
     * so ZAV_KAFEDRA can see who to assign units to without full account-management access. */
    @GetMapping("/professors")
    public ResponseEntity<?> listProfessors(Authentication auth) {
        if (!accessControl.isSuperAdminOrZavKafedra(auth))
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        List<Map<String, Object>> professors = adminUserRepository.findAll().stream()
            .filter(u -> AdminUser.ROLE_PROFESSOR.equals(u.getRole()))
            .map(u -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", u.getId());
                m.put("username", u.getUsername());
                m.put("fullName", u.getFullName() != null ? u.getFullName() : "");
                return m;
            })
            .toList();
        return ResponseEntity.ok(professors);
    }

    @GetMapping("/{adminUserId}")
    public ResponseEntity<?> list(@PathVariable int adminUserId, Authentication auth) {
        if (!accessControl.isSuperAdminOrZavKafedra(auth) && !isSelf(auth, adminUserId))
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        return ResponseEntity.ok(adminUserRepository.getAssignmentsWithUnitNames(adminUserId));
    }

    @PostMapping
    public ResponseEntity<?> assign(@RequestBody Map<String, Object> body, Authentication auth) {
        if (!accessControl.isSuperAdminOrZavKafedra(auth))
            return ResponseEntity.status(403).body(Map.of("error", "Only zav kafedra or super admin can assign units"));

        Object adminUserIdObj = body.get("adminUserId");
        Object unitIdObj = body.get("unitId");
        if (adminUserIdObj == null || unitIdObj == null)
            return ResponseEntity.badRequest().body(Map.of("error", "adminUserId and unitId required"));
        int adminUserId = Integer.parseInt(adminUserIdObj.toString());
        int unitId = Integer.parseInt(unitIdObj.toString());

        AdminUser targetUser = adminUserRepository.findAll().stream()
            .filter(u -> u.getId() == adminUserId).findFirst().orElse(null);
        if (targetUser == null) return ResponseEntity.notFound().build();
        if (!AdminUser.ROLE_PROFESSOR.equals(targetUser.getRole()))
            return ResponseEntity.badRequest().body(Map.of("error", "Only PROFESSOR accounts can be assigned units"));

        Integer assignedBy = adminUserRepository.findByUsername(auth.getName()).map(AdminUser::getId).orElse(null);
        adminUserRepository.assignUnit(adminUserId, unitId, assignedBy);
        return ResponseEntity.ok(adminUserRepository.getAssignmentsWithUnitNames(adminUserId));
    }

    @DeleteMapping("/{adminUserId}/{unitId}")
    public ResponseEntity<?> unassign(@PathVariable int adminUserId, @PathVariable int unitId, Authentication auth) {
        if (!accessControl.isSuperAdminOrZavKafedra(auth))
            return ResponseEntity.status(403).body(Map.of("error", "Only zav kafedra or super admin can unassign units"));
        adminUserRepository.unassignUnit(adminUserId, unitId);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private boolean isSelf(Authentication auth, int adminUserId) {
        return adminUserRepository.findByUsername(auth.getName())
            .map(u -> u.getId() == adminUserId).orElse(false);
    }
}
