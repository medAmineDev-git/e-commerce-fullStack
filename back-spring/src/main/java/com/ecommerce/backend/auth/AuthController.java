package com.ecommerce.backend.auth;

import com.ecommerce.backend.auth.dto.LoginRequest;
import com.ecommerce.backend.auth.dto.LoginResponse;
import com.ecommerce.backend.auth.dto.RegisterStoreRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register-store")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerStore(@Valid @RequestBody RegisterStoreRequest request) {
        return authService.registerStore(request);
    }
}
