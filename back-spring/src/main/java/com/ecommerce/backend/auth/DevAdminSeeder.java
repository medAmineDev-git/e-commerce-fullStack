package com.ecommerce.backend.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Un unique compte d'exploitation, en developpement seulement.
 *
 * Les boutiques se creent par le formulaire public, comme en production : les
 * pre-remplir donnerait un environnement de developpement qui ne ressemble pas
 * a ce que vivent les vendeurs.
 *
 * Rien n'est cree en production : des identifiants connus d'avance sur une
 * instance publique seraient une porte ouverte. Le premier exploitant s'y
 * obtient par la variable APP_PLATFORM_OWNER, voir {@link PlatformOwnerBootstrap}.
 */
@Component
@Profile("dev")
public class DevAdminSeeder implements ApplicationRunner {

    private static final String USERNAME = "superadmin";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminSeeder(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        AdminUser user = adminUserRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(USERNAME, USERNAME + "@ecommerce.local")
                .orElseGet(AdminUser::new);

        user.setUsername(USERNAME);
        user.setEmail(USERNAME + "@ecommerce.local");
        user.setPasswordHash(passwordEncoder.encode(USERNAME));
        user.setRole(Role.SUPER_ADMIN.authority());

        adminUserRepository.save(user);
    }
}
