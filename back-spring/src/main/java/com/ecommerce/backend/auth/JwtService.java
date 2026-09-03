package com.ecommerce.backend.auth;

import com.ecommerce.backend.store.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Jetons signes HMAC-SHA256, charge utile JSON.
 *
 * Le jeton porte desormais la boutique. Avant, le perimetre etait redecouvert a
 * chaque requete par une recherche sur le nom d'utilisateur : couteux, et surtout
 * dependant d'une donnee modifiable plutot que d'une donnee signee.
 *
 * Deux types de jetons : un jeton d'acces court, et un jeton de rafraichissement
 * long qui ne porte aucun perimetre — il ne sert qu'a en obtenir un nouveau, ce
 * qui donne un point de passage regulier pour revoir les droits.
 */
@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final byte[] secret;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-expiration-seconds:900}") long accessExpirationSeconds,
            @Value("${app.security.jwt-refresh-expiration-seconds:2592000}") long refreshExpirationSeconds,
            ObjectMapper objectMapper
    ) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(AdminUser user, Store store) {
        return encode(new TokenClaims(
                user.getUsername(),
                user.getRole(),
                store != null ? store.getId() : null,
                store != null ? store.getSlug() : null,
                TYPE_ACCESS,
                Instant.now().getEpochSecond() + accessExpirationSeconds
        ));
    }

    public String createRefreshToken(AdminUser user) {
        return encode(new TokenClaims(
                user.getUsername(),
                user.getRole(),
                null,
                null,
                TYPE_REFRESH,
                Instant.now().getEpochSecond() + refreshExpirationSeconds
        ));
    }

    /** @return les claims, ou null si le jeton est invalide, expire, ou du mauvais type. */
    public TokenClaims parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public TokenClaims parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    public long accessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    private String encode(TokenClaims claims) {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsString(claims).getBytes(StandardCharsets.UTF_8));
        return payload + "." + sign(payload);
    }

    private TokenClaims parse(String token, String expectedType) {
        if (token == null) {
            return null;
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            return null;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            TokenClaims claims = objectMapper.readValue(payload, TokenClaims.class);

            if (claims == null
                    || !expectedType.equals(claims.type())
                    || claims.exp() < Instant.now().getEpochSecond()) {
                return null;
            }
            return claims;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
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

    public record TokenClaims(
            String username,
            String role,
            Long storeId,
            String storeSlug,
            String type,
            long exp
    ) {
    }
}
