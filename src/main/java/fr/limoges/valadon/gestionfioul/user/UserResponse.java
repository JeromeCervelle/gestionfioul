package fr.limoges.valadon.gestionfioul.user;

/**
 * Vue publique d'un utilisateur : le mot de passe n'y figure jamais.
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
