package fr.limoges.valadon.gestionfioul.proposition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corps JSON de {@code POST /api/campagnes/{campagneId}/propositions}.
 * Pas de {@code fournisseurId} : c'est l'utilisateur connecté.
 *
 * @param validiteJusqua date jusqu'à laquelle les prix sont garantis
 * @param paliers        tranches de volume et leurs prix
 */
public record SoumettrePropositionRequest(
        LocalDateTime validiteJusqua,
        List<Palier> paliers) {
}
