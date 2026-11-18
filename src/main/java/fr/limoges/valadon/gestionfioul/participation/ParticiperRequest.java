package fr.limoges.valadon.gestionfioul.participation;

/**
 * Corps JSON de {@code POST /api/campagnes/{campagneId}/participations}.
 * Pas de {@code clientId} : c'est l'utilisateur connecté.
 *
 * @param volumeSouhaite   volume en litres
 * @param adresseLivraison adresse de livraison, facultative
 */
public record ParticiperRequest(
        int volumeSouhaite,
        String adresseLivraison) {
}
