package com.ecommerce.backend.auth;

import com.ecommerce.backend.auth.dto.LoginRequest;
import com.ecommerce.backend.auth.dto.LoginResponse;
import com.ecommerce.backend.auth.dto.RefreshRequest;
import com.ecommerce.backend.auth.dto.RegisterStoreRequest;
import com.ecommerce.backend.store.Store;
import com.ecommerce.backend.store.StoreRepository;
import com.ecommerce.backend.store.StoreService;
import com.ecommerce.backend.store.dto.StoreCreateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username, email or password";

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
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException(INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    /**
     * Le rafraichissement est le point de passage ou les droits sont revus :
     * un compte supprime ou une boutique desactivee cessent d'obtenir des jetons.
     */
    public LoginResponse refresh(RefreshRequest request) {
        JwtService.TokenClaims claims = jwtService.parseRefreshToken(request.refreshToken());
        if (claims == null) {
            throw new InvalidCredentialsException("Refresh token is invalid or expired");
        }

        AdminUser user = adminUserRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(claims.username(), claims.username())
                .orElseThrow(() -> new InvalidCredentialsException("Account no longer exists"));

        return issueTokens(user);
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
        user.setRole(Role.STORE_OWNER.authority());
        AdminUser savedUser = adminUserRepository.save(user);

        StoreCreateRequest storeCreateRequest = new StoreCreateRequest(
                request.storeName().trim(),
                request.storeSlug(),
                request.description(),
                null,
                null,
                null,
                null,
                email,
                null,
                null
        );

        Store createdStore = storeService.createStore(savedUser, storeCreateRequest);
        return tokensFor(savedUser, createdStore);
    }

    private LoginResponse issueTokens(AdminUser user) {
        Store store = storeRepository.findFirstByOwner(user).orElse(null);

        // Une boutique desactivee par la plateforme ne delivre plus de jeton :
        // sinon son proprietaire continuerait a l'administrer normalement.
        if (store != null && !store.isActive()) {
            throw new InvalidCredentialsException("This store has been deactivated");
        }

        return tokensFor(user, store);
    }

    private LoginResponse tokensFor(AdminUser user, Store store) {
        return new LoginResponse(
                jwtService.createAccessToken(user, store),
                jwtService.createRefreshToken(user),
                jwtService.accessExpirationSeconds(),
                user.getUsername(),
                user.getRole(),
                store != null ? store.getId() : null,
                store != null ? store.getSlug() : null
        );
    }
}
