package fr.limoges.valadon.gestionfioul.user;

import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    public User trouverParId(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + id));
    }

    public User trouverParEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("Utilisateur introuvable : " + email));
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
