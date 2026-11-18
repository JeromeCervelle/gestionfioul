package fr.limoges.valadon.gestionfioul.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;
import fr.limoges.valadon.gestionfioul.user.UserRepository;
import fr.limoges.valadon.gestionfioul.user.UserService;

/**
 * Crée le compte administrateur au démarrage s'il n'existe pas encore.
 *
 * <p>Un administrateur ne peut pas s'inscrire par l'API : c'est la seule voie pour en obtenir un.
 * Email et mot de passe viennent des propriétés {@code gestionfioul.admin.email} et
 * {@code gestionfioul.admin.password} (variable d'environnement {@code ADMIN_PASSWORD}).</p>
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserService userService;
    private final UserRepository userRepository;
    private final String email;
    private final String motDePasse;

    /**
     * Crée l'initialiseur.
     *
     * @param userService    service d'inscription (validation et hachage du mot de passe)
     * @param userRepository pour savoir si le compte existe déjà
     * @param email          email du compte administrateur
     * @param motDePasse     mot de passe ; vide : aucun compte n'est créé
     */
    public AdminInitializer(UserService userService,
                            UserRepository userRepository,
                            @Value("${gestionfioul.admin.email}") String email,
                            @Value("${gestionfioul.admin.password:}") String motDePasse) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.email = email;
        this.motDePasse = motDePasse;
    }

    /**
     * Crée le compte au démarrage de l'application, sauf si le mot de passe est vide ou si le compte existe.
     *
     * @param args arguments de la ligne de commande (inutilisés)
     */
    @Override
    public void run(ApplicationArguments args) {
        if (motDePasse.isBlank()) {
            log.warn("Aucun compte admin créé : définir la variable d'environnement ADMIN_PASSWORD");
            return;
        }
        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setMotDePasse(motDePasse);
        admin.setNom("Administrateur");
        admin.setRole(Role.ADMIN);
        userService.inscrire(admin);
        log.info("Compte admin créé : {}", email);
    }
}
