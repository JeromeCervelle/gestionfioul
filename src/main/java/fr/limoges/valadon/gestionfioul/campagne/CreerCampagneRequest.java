package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;

public record CreerCampagneRequest(
        String libelle,
        LocalDateTime dateOuverture,
        LocalDateTime dateCloture,
        String zoneLivraison) {
}
