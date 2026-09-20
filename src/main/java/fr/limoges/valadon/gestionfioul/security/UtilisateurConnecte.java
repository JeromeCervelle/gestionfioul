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
 * Il porte l'id pour que les contrôleurs sachent qui agit, sans le lire dans la requête.
 */
public class UtilisateurConnecte implements UserDetails {

    private final String id;
    private final String email;
    private final String motDePasse;
    private final Role role;

    public UtilisateurConnecte(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.motDePasse = user.getMotDePasse();
        this.role = user.getRole();
    }

    public String getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return motDePasse;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
