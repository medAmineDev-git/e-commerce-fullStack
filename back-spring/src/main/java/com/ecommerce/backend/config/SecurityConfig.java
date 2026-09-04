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
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;


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
                /*
                 * Spring Security interdit toute mise en cache par defaut, sur
                 * chaque reponse. C'est le bon comportement pour du JSON
                 * authentifie, mais il s'appliquait aussi aux scripts du site :
                 * le navigateur retelechargeait plus d'un megaoctet a chaque
                 * visite.
                 *
                 * On desactive donc la regle globale, et on la remet sur les
                 * seules routes d'API. Les fichiers statiques recuperent alors
                 * les durees declarees dans WebConfig.
                 */
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                                new CacheControlHeadersWriter()))
                )
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
                                "/uploads/**"
                        ).permitAll()
                        // Deux roles seulement depuis V110. ROLE_ADMIN a disparu :
                        // c'etait un role de boutique qui ouvrait la console plateforme.
                        .requestMatchers("/api/platform/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/dev/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("STORE_OWNER", "SUPER_ADMIN")
                        // Toute autre route sous /api est refusee explicitement, avant
                        // que la regle suivante n'ouvre le reste : sans cette ligne,
                        // un futur endpoint mal place serait servi sans controle.
                        .requestMatchers("/api/**").denyAll()
                        // Le site lui-meme, embarque dans le jar : pages, scripts,
                        // feuilles de style et documentation de l'API.
                        .anyRequest().permitAll()
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
