package fr.limoges.valadon.gestionfioul.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.limoges.valadon.gestionfioul.security.UtilisateurConnecte;

/**
 * Routes {@code /api/users} : inscription et consultation des profils.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code POST /api/users} : création d'un compte. Route publique (voir {@link fr.limoges.valadon.gestionfioul.security.SecurityConfig SecurityConfig}).
     *
     * @param request données du compte
     * @return le profil créé (201), sans mot de passe ; 400 si une donnée est invalide, si l'email est déjà
     *         utilisé ou si le rôle {@code ADMIN} est demandé
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse inscrire(@RequestBody InscriptionRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Le rôle ADMIN ne peut pas être demandé à l'inscription");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setMotDePasse(request.motDePasse());
        user.setNom(request.nom());
        user.setPrenom(request.prenom());
        user.setTelephone(request.telephone());
        user.setRole(request.role());
        user.setAdresse(request.adresse());
        user.setRaisonSociale(request.raisonSociale());
        return UserResponse.from(userService.inscrire(user));
    }

    /**
     * {@code GET /api/users/moi} : profil de l'utilisateur connecté.
     *
     * <p>Permet à une application cliente (le site web, une appli mobile...) de vérifier des identifiants
     * et de connaître l'id et le rôle qui leur correspondent : 200 si les identifiants sont bons, 401 sinon.</p>
     *
     * <p>{@code /moi} est un chemin littéral : Spring le préfère au motif {@code /{id}}, il n'y a pas de conflit.</p>
     *
     * @param utilisateur utilisateur authentifié
     * @return son profil
     */
    @GetMapping("/moi")
    public UserResponse moi(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return UserResponse.from(userService.trouverParId(utilisateur.getId()));
    }

    /**
     * {@code GET /api/users/{id}} : un utilisateur ne voit que son propre profil, l'administrateur les voit tous.
     *
     * @param id identifiant de l'utilisateur
     * @return le profil ; 403 pour le profil d'un autre utilisateur, 404 s'il n'existe pas
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public UserResponse trouver(@PathVariable String id) {
        return UserResponse.from(userService.trouverParId(id));
    }
}
