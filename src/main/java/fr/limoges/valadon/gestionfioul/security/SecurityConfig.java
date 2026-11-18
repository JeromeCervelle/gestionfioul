package fr.limoges.valadon.gestionfioul.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité de l'API.
 *
 * <ul>
 *   <li>HTTP Basic : email et mot de passe envoyés à CHAQUE requête, aucune session ni cookie ;</li>
 *   <li>CSRF désactivé : sans cookie, le navigateur n'envoie rien tout seul, l'attaque est impossible ;</li>
 *   <li>routes publiques : l'inscription ({@code POST /api/users}) et la documentation Swagger ;
 *       tout le reste exige d'être authentifié ;</li>
 *   <li>droits par rôle : {@code @PreAuthorize} sur les méthodes des contrôleurs, activé par
 *       {@code @EnableMethodSecurity}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity   // active les @PreAuthorize placés sur les contrôleurs
public class SecurityConfig {

    /**
     * Règles d'accès et mode d'authentification.
     *
     * @param http configuration de la sécurité HTTP, fournie par Spring Security
     * @return la chaîne de filtres de sécurité
     * @throws Exception si la configuration est invalide
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API sans session ni cookie : le navigateur n'envoie rien tout seul, donc pas de risque CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()   // inscription
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()   // documentation
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Hachage des mots de passe avec BCrypt (salé, volontairement lent).
     *
     * @return l'encodeur utilisé à l'inscription et à la vérification des identifiants
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
