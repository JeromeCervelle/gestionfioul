package fr.limoges.valadon.gestionfioul.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import fr.limoges.valadon.gestionfioul.user.UserRepository;

/**
 * Indique à Spring Security comment retrouver un utilisateur à partir de son email (le "username").
 */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public UtilisateurDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(UtilisateurConnecte::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));
    }
}
