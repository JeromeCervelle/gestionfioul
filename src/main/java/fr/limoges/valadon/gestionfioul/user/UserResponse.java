package fr.limoges.valadon.gestionfioul.user;

/**
 * Vue publique d'un utilisateur : le mot de passe n'y figure jamais.
 *
 * @param id            identifiant
 * @param email         email de connexion
 * @param nom           nom de famille
 * @param prenom        prénom
 * @param telephone     téléphone
 * @param role          rôle
 * @param adresse       adresse postale
 * @param raisonSociale nom de l'entreprise (fournisseur)
 */
public record UserResponse(
        String id,
        String email,
        String nom,
        String prenom,
        String telephone,
        Role role,
        String adresse,
        String raisonSociale) {

    /**
     * Copie les champs publics d'un utilisateur.
     *
     * @param user document MongoDB
     * @return la vue publique, sans mot de passe
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNom(),
                user.getPrenom(),
                user.getTelephone(),
                user.getRole(),
                user.getAdresse(),
                user.getRaisonSociale());
    }
}
