package fr.limoges.valadon.gestionfioul.user;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Accès aux utilisateurs dans MongoDB.
 *
 * <p>Spring Data écrit lui-même les requêtes à partir du NOM des méthodes
 * ({@code findBy...}, {@code existsBy...}).</p>
 */
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Cherche un utilisateur par son email.
     *
     * @param email email, en minuscules
     * @return l'utilisateur, ou un {@code Optional} vide
     */
    Optional<User> findByEmail(String email);

    /**
     * Indique si un compte utilise déjà cet email.
     *
     * @param email email, en minuscules
     * @return {@code true} si un compte existe
     */
    boolean existsByEmail(String email);
}
