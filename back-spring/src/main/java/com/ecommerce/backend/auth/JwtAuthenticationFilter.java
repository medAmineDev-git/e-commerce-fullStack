package com.ecommerce.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Reprise dans les journaux : sans elle, un incident multi-boutique est indiagnostiquable. */
    public static final String STORE_MDC_KEY = "storeId";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            JwtService.TokenClaims claims = jwtService.parseAccessToken(authorization.substring(7));

            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedUser principal = new AuthenticatedUser(
                        claims.username(), claims.role(), claims.storeId(), claims.storeSlug());

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(claims.role()))
                        )
                );

                if (claims.storeId() != null) {
                    MDC.put(STORE_MDC_KEY, String.valueOf(claims.storeId()));
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(STORE_MDC_KEY);
        }
    }
}
