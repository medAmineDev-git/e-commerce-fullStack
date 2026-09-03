package com.ecommerce.backend.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Deux comptes de developpement, un par role.
 * ROLE_ADMIN a disparu avec V110 : il donnait l'acces a la console plateforme
 * a des proprietaires de boutique.
 */
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
        upsert("admin", "admin@ecommerce.local", "admin", Role.STORE_OWNER);
        upsert("platform", "platform@ecommerce.local", "platform", Role.SUPER_ADMIN);
    }

    private void upsert(String username, String email, String password, Role role) {
        AdminUser user = adminUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email)
                .orElseGet(AdminUser::new);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role.authority());
        adminUserRepository.save(user);
    }
}
