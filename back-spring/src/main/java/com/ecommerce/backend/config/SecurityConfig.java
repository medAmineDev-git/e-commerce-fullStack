package com.ecommerce.backend.config;

import com.ecommerce.backend.auth.JwtAuthenticationFilter;
import com.ecommerce.backend.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Trois surfaces, et rien entre elles :
 *
 *   /api/public/stores/{slug}/**  vitrine, anonyme, perimetre donne par le slug
 *   /api/admin/**                 back-office, perimetre donne par le compte
 *   /api/platform/**              exploitation de la plateforme
 *
 * Les anciennes routes /api/products, /api/categories, /api/orders et
 * /api/home/configuration ont ete retirees : elles servaient le catalogue de
 * toutes les boutiques confondues des lors que l'appelant etait anonyme.
 */
@Configuration
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/swagger.html",
                                "/openapi.yaml",
                                "/uploads/**"
                        ).permitAll()
                        // Deux roles seulement depuis V110. ROLE_ADMIN a disparu :
                        // c'etait un role de boutique qui ouvrait la console plateforme.
                        .requestMatchers("/api/platform/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/dev/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("STORE_OWNER", "SUPER_ADMIN")
                        .anyRequest().denyAll()
                )
                // Sans ceci, une requete anonyme recoit 403 : le client ne peut pas
                // distinguer "connecte-toi" de "tu n'as pas le droit", et ne sait donc
                // pas s'il doit rafraichir son jeton.
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required"))
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
