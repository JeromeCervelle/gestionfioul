package fr.limoges.valadon.gestionfioul.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import fr.limoges.valadon.gestionfioul.user.UserRepository;

/**
 * Indique à Spring Security comment retrouver un utilisateur à partir de son email (le « username »).
 *
 * <p>Appelé à chaque requête authentifiée : Spring Security compare ensuite le mot de passe reçu
 * au haché enregistré.</p>
 */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Crée le service.
     *
     * @param userRepository accès aux utilisateurs dans MongoDB
     */
    public UtilisateurDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Cherche l'utilisateur par son email (sans tenir compte de la casse ni des espaces autour).
     *
     * @param email email reçu dans l'en-tête HTTP Basic
     * @return l'utilisateur, sous forme de {@link UtilisateurConnecte}
     * @throws UsernameNotFoundException si aucun compte n'a cet email (réponse 401)
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(UtilisateurConnecte::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
    }
}
