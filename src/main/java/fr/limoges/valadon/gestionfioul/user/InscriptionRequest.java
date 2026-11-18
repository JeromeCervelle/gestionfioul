package fr.limoges.valadon.gestionfioul.user;

/**
 * Corps JSON de {@code POST /api/users}.
 *
 * @param email         email, qui servira d'identifiant de connexion
 * @param motDePasse    mot de passe en clair (8 caractères minimum), haché avant l'enregistrement
 * @param nom           nom de famille
 * @param prenom        prénom, facultatif
 * @param telephone     téléphone, facultatif
 * @param role          {@code CLIENT} ou {@code FOURNISSEUR} ; {@code ADMIN} est refusé
 * @param adresse       adresse de livraison (client) ou du siège (fournisseur)
 * @param raisonSociale nom de l'entreprise, obligatoire pour un fournisseur
 */
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
