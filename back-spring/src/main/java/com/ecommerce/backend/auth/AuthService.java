package com.ecommerce.backend.auth;

import com.ecommerce.backend.auth.dto.LoginRequest;
import com.ecommerce.backend.auth.dto.LoginResponse;
import com.ecommerce.backend.auth.dto.RegisterStoreRequest;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import com.ecommerce.backend.store.StoreService;
import com.ecommerce.backend.store.dto.StoreCreateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StoreRepository storeRepository;
    private final StoreService storeService;

    public AuthService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            StoreRepository storeRepository,
            StoreService storeService
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.storeRepository = storeRepository;
        this.storeService = storeService;
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        AdminUser user = adminUserRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username, email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username, email or password");
        }

        String storeSlug = storeRepository.findFirstByOwner(user)
                .map(Store::getSlug)
                .orElse("nova");

        return new LoginResponse(jwtService.createToken(user), user.getUsername(), user.getRole(), storeSlug);
    }

    @Transactional
    public LoginResponse registerStore(RegisterStoreRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();

        if (adminUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_STORE_OWNER");
        AdminUser savedUser = adminUserRepository.save(user);

        StoreCreateRequest storeCreateRequest = new StoreCreateRequest(
                request.storeName().trim(),
                request.storeSlug(),
                request.description(),
                null,
                null,
                null,
                email,
                null,
                null
        );

        Store createdStore = storeService.createStore(savedUser, storeCreateRequest);

        return new LoginResponse(
                jwtService.createToken(savedUser),
                savedUser.getUsername(),
                savedUser.getRole(),
                createdStore.getSlug()
        );
    }
}
