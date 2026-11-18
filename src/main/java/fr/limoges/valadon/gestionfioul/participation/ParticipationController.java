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

/**
 * Routes des participations.
 *
 * <p>Le client est toujours l'utilisateur connecté ({@code @AuthenticationPrincipal}) :
 * son id n'est jamais lu dans la requête.</p>
 */
@RestController
@RequestMapping("/api")
public class ParticipationController {

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    /**
     * {@code POST /api/campagnes/{campagneId}/participations} (client) : s'engager sur un volume.
     *
     * @param campagneId  identifiant de la campagne
     * @param request     volume et adresse de livraison (vide : adresse du profil)
     * @param utilisateur client connecté
     * @return la participation créée (201) ; 400 si le volume est invalide,
     *         409 si la campagne n'est pas ouverte ou si le client y participe déjà
     */
    @PostMapping("/campagnes/{campagneId}/participations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public Participation participer(@PathVariable String campagneId,
                                    @RequestBody ParticiperRequest request,
                                    @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return participationService.participer(
                campagneId, utilisateur.getId(), request.volumeSouhaite(), request.adresseLivraison());
    }

    /**
     * {@code GET /api/campagnes/{campagneId}/participations} (administrateur) : les commandes de tous les clients.
     *
     * @param campagneId identifiant de la campagne
     * @return toutes les participations, annulées comprises
     */
    @GetMapping("/campagnes/{campagneId}/participations")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Participation> listerParCampagne(@PathVariable String campagneId) {
        return participationService.listerParCampagne(campagneId);
    }

    /**
     * {@code GET /api/moi/participations} (client) : historique du client connecté.
     *
     * @param utilisateur client connecté
     * @return ses participations, toutes campagnes confondues
     */
    @GetMapping("/moi/participations")
    @PreAuthorize("hasRole('CLIENT')")
    public List<Participation> mesParticipations(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return participationService.listerParClient(utilisateur.getId());
    }

    /**
     * {@code POST /api/participations/{id}/annuler} (client) : retrait avant la date limite.
     *
     * @param id          identifiant de la participation
     * @param utilisateur client connecté, qui doit être celui de la participation
     * @return la participation annulée ; 400 si elle appartient à un autre client,
     *         409 si elle est déjà annulée ou si la campagne n'est plus ouverte
     */
    @PostMapping("/participations/{id}/annuler")
    @PreAuthorize("hasRole('CLIENT')")
    public Participation annuler(@PathVariable String id,
                                 @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return participationService.annuler(id, utilisateur.getId());
    }
}
