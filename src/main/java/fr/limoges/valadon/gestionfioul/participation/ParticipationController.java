package fr.limoges.valadon.gestionfioul.participation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.limoges.valadon.gestionfioul.security.UtilisateurConnecte;

@RestController
@RequestMapping("/api")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    // le client est celui qui est connecté : on ne lit jamais son id dans la requête
    @PostMapping("/campagnes/{campagneId}/participations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public Participation participer(@PathVariable String campagneId,
                                    @RequestBody ParticiperRequest request,
                                    @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return participationService.participer(
                campagneId, utilisateur.getId(), request.volumeSouhaite(), request.adresseLivraison());
    }

    // les commandes de tous les clients : réservé à l'admin
    @GetMapping("/campagnes/{campagneId}/participations")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Participation> listerParCampagne(@PathVariable String campagneId) {
        return participationService.listerParCampagne(campagneId);
    }

    @GetMapping("/moi/participations")
    @PreAuthorize("hasRole('CLIENT')")
    public List<Participation> mesParticipations(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return participationService.listerParClient(utilisateur.getId());
    }

    @PostMapping("/participations/{id}/annuler")
    @PreAuthorize("hasRole('CLIENT')")
    public Participation annuler(@PathVariable String id,
                                 @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return participationService.annuler(id, utilisateur.getId());
    }
}
