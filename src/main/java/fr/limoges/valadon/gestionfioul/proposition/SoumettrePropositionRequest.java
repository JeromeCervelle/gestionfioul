package fr.limoges.valadon.gestionfioul.proposition;

import java.time.LocalDateTime;
import java.util.List;

public record SoumettrePropositionRequest(
        LocalDateTime validiteJusqua,
        List<Palier> paliers) {
}
