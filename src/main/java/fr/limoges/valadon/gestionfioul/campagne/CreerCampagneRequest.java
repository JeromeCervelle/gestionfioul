package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;

/**
 * Corps JSON de {@code POST /api/campagnes}. Le statut, le volume et la proposition retenue sont fixés par l'API.
 *
 * @param libelle       nom de la campagne
 * @param dateOuverture date d'ouverture aux participations
 * @param dateCloture   date limite de participation, postérieure à l'ouverture
 * @param zoneLivraison zone desservie, facultative
 */
public record CreerCampagneRequest(
        String libelle,
        LocalDateTime dateOuverture,
        LocalDateTime dateCloture,
        String zoneLivraison) {
}
