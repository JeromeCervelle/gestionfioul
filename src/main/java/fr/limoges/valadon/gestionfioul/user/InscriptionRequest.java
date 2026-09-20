package fr.limoges.valadon.gestionfioul.user;

public record InscriptionRequest(
        String email,
        String motDePasse,
        String nom,
        String prenom,
        String telephone,
        Role role,
        String adresse,
        String raisonSociale) {
}
