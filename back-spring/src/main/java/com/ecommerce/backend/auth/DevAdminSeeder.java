package com.ecommerce.backend.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminSeeder(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        AdminUser admin = adminUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("admin", "admin@ecommerce.local")
                .orElseGet(AdminUser::new);
        admin.setUsername("admin");
        admin.setEmail("admin@ecommerce.local");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setRole("ROLE_ADMIN");
        adminUserRepository.save(admin);
    }
}
