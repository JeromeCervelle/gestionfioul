package fr.limoges.valadon.gestionfioul.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // route publique (voir SecurityConfig)
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

    // un utilisateur ne voit que son propre profil, l'admin voit tous les profils
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public UserResponse trouver(@PathVariable String id) {
        return UserResponse.from(userService.trouverParId(id));
    }
}
