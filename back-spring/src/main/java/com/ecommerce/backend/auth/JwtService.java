package com.ecommerce.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-expiration-seconds:28800}") long expirationSeconds
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(AdminUser user) {
        long expiration = Instant.now().getEpochSecond() + expirationSeconds;
        String payload = user.getUsername() + ":" + user.getRole() + ":" + expiration;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    public JwtPrincipal parseToken(String token) {
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] claims = payload.split(":", 3);
            if (claims.length != 3 || Long.parseLong(claims[2]) < Instant.now().getEpochSecond()) {
                return null;
            }
            return new JwtPrincipal(claims[0], claims[1]);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign authentication token", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record JwtPrincipal(String username, String role) {
    }
}
