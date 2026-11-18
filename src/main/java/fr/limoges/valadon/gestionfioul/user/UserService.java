package fr.limoges.valadon.gestionfioul.user;

import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Règles métier des comptes utilisateurs.
 *
 * <p>Les erreurs sont levées sous forme d'exceptions standard, traduites en HTTP par {@link fr.limoges.valadon.gestionfioul.common.GestionExceptionHandler GestionExceptionHandler}.</p>
 */
@Service
public class UserService {

    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Inscrit un utilisateur : contrôle les données, met l'email en minuscules, hache le mot de passe.
     *
     * @param user utilisateur à créer, mot de passe en clair
     * @return l'utilisateur enregistré, avec son id
     * @throws IllegalArgumentException email absent, mot de passe trop court (moins de 8 caractères),
     *         rôle absent, raison sociale absente pour un fournisseur, email déjà utilisé
     */
    public User inscrire(User user) {
        if (isBlank(user.getEmail())) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (user.getMotDePasse() == null || user.getMotDePasse().length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Le rôle est obligatoire");
        }
        if (user.getRole() == Role.FOURNISSEUR && isBlank(user.getRaisonSociale())) {
            throw new IllegalArgumentException("La raison sociale est obligatoire pour un fournisseur");
        }
        if (user.getRole() == Role.CLIENT) {
            user.setRaisonSociale(null);
        }

        user.setEmail(user.getEmail().trim().toLowerCase());
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }

        user.setMotDePasse(passwordEncoder.encode(user.getMotDePasse()));
        return userRepository.save(user);
    }

    /**
     * Cherche un utilisateur par son id.
     *
     * @param id identifiant
     * @return l'utilisateur
     * @throws NoSuchElementException si l'utilisateur n'existe pas
     */
    public User trouverParId(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + id));
    }

    /**
     * Cherche un utilisateur par son email (sans tenir compte de la casse ni des espaces autour).
     *
     * @param email email
     * @return l'utilisateur
     * @throws NoSuchElementException si aucun compte n'a cet email
     */
    public User trouverParEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + email));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
