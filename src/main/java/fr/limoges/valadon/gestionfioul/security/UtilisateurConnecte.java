package fr.limoges.valadon.gestionfioul.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;

/**
 * Utilisateur authentifié tel que Spring Security le connaît.
 *
 * <p>Il porte l'id pour que les contrôleurs sachent qui agit, sans le lire dans la requête :
 * un client ne peut donc pas agir au nom d'un autre. Les contrôleurs le reçoivent par
 * {@code @AuthenticationPrincipal}.</p>
 */
public class UtilisateurConnecte implements UserDetails {

    private final String id;
    private final String email;
    private final String motDePasse;
    private final Role role;

    /**
     * Construit l'utilisateur authentifié à partir du document MongoDB.
     *
     * @param user utilisateur trouvé par son email
     */
    public UtilisateurConnecte(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.motDePasse = user.getMotDePasse();
        this.role = user.getRole();
    }

    /**
     * Identifiant MongoDB de l'utilisateur.
     *
     * @return l'id, utilisé par les contrôleurs et dans {@code @PreAuthorize("... #id == principal.id")}
     */
    public String getId() {
        return id;
    }

    /**
     * Rôle de l'utilisateur.
     *
     * @return le rôle
     */
    public Role getRole() {
        return role;
    }

    /**
     * Autorités Spring Security : le préfixe {@code ROLE_} permet d'écrire {@code hasRole('ADMIN')}.
     *
     * @return une seule autorité, {@code ROLE_ADMIN}, {@code ROLE_CLIENT} ou {@code ROLE_FOURNISSEUR}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Mot de passe haché, comparé par Spring Security à celui reçu dans la requête.
     *
     * @return le haché BCrypt
     */
    @Override
    public String getPassword() {
        return motDePasse;
    }

    /**
     * Identifiant de connexion.
     *
     * @return l'email
     */
    @Override
    public String getUsername() {
        return email;
    }
}
