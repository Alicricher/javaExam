package com.dentistrybot.admin;

import com.dentistrybot.admin.model.AdminUser;
import com.dentistrybot.admin.repository.AdminUserRepository;
import com.dentistrybot.shared.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public AdminUserSeeder(AdminUserRepository adminUserRepository,
                           PasswordEncoder passwordEncoder,
                           AppProperties appProperties) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialAdmin() {
        if (adminUserRepository.count() == 0) {
            String rawPassword = appProperties.getAdminPassword();
            String encoded = rawPassword.startsWith("$2") ? rawPassword : passwordEncoder.encode(rawPassword);
            AdminUser superAdmin = new AdminUser();
            superAdmin.setUsername("admin");
            superAdmin.setPasswordHash(encoded);
            superAdmin.setRole(AdminUser.ROLE_SUPER_ADMIN);
            superAdmin.setFullName("Super Admin");
            adminUserRepository.create(superAdmin);
            log.info("Seeded initial superadmin user 'admin'");
        }
    }
}
