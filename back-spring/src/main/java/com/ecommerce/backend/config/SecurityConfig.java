package com.ecommerce.backend.config;

import com.ecommerce.backend.auth.JwtAuthenticationFilter;
import com.ecommerce.backend.auth.JwtService;
import jakarta.servlet.DispatcherType;
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
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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
                        // La redispatch interne vers /error doit traverser la chaine
                        // sans etre reevaluee. Sans cette ligne, sendError(403)
                        // repasse par denyAll() et ressort en 401 : le client ne peut
                        // plus distinguer "connecte-toi" de "tu n'as pas le droit".
                        // MockMvc ne simule pas cette redispatch, d'ou des tests verts
                        // sur un comportement faux en conditions reelles.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
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
                        // 401 : personne n'est identifie, il faut se connecter ou
                        // rafraichir son jeton.
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication required"))
                        // 403 : l'appelant est identifie mais n'a pas le droit.
                        // Sans ce gestionnaire explicite, le refus retombait sur le
                        // point d'entree ci-dessus et sortait en 401 : le client
                        // tentait alors un rafraichissement qui ne pouvait rien changer.
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value(), "Access denied"))
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Les origines autorisees sont calculees par StoreAwareCorsConfigurationSource,
    // a partir des domaines enregistres en base plutot que d'une liste figee.
}
