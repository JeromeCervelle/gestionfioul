package fr.limoges.valadon.gestionfioul.security;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;

/**
 * Authentifie une requête MockMvc comme un utilisateur de chaque rôle,
 * avec un {@link UtilisateurConnecte} : les contrôleurs y lisent l'id de l'appelant.
 */
public final class UtilisateursDeTest {

    public static final String ID_ADMIN = "admin-1";
    public static final String ID_CLIENT = "client-1";
    public static final String ID_AUTRE_CLIENT = "client-2";
    public static final String ID_FOURNISSEUR = "fournisseur-1";

    private UtilisateursDeTest() {
    }

    public static RequestPostProcessor admin() {
        return connecte(ID_ADMIN, Role.ADMIN);
    }

    public static RequestPostProcessor client() {
        return connecte(ID_CLIENT, Role.CLIENT);
    }

    public static RequestPostProcessor autreClient() {
        return connecte(ID_AUTRE_CLIENT, Role.CLIENT);
    }

    public static RequestPostProcessor fournisseur() {
        return connecte(ID_FOURNISSEUR, Role.FOURNISSEUR);
    }

    private static RequestPostProcessor connecte(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.fr");
        user.setMotDePasse("hache");
        user.setRole(role);
        return SecurityMockMvcRequestPostProcessors.user(new UtilisateurConnecte(user));
    }
}
