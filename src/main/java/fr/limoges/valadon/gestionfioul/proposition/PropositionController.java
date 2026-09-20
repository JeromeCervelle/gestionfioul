package fr.limoges.valadon.gestionfioul.proposition;

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
public class PropositionController {

    private final PropositionService propositionService;

    public PropositionController(PropositionService propositionService) {
        this.propositionService = propositionService;
    }

    // le fournisseur est celui qui est connecté : on ne lit jamais son id dans la requête
    @PostMapping("/campagnes/{campagneId}/propositions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FOURNISSEUR')")
    public Proposition soumettre(@PathVariable String campagneId,
                                 @RequestBody SoumettrePropositionRequest request,
                                 @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return propositionService.soumettre(
                campagneId, utilisateur.getId(), request.validiteJusqua(), request.paliers());
    }

    // les barèmes des concurrents restent confidentiels : réservé à l'admin
    @GetMapping("/campagnes/{campagneId}/propositions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Proposition> listerParCampagne(@PathVariable String campagneId) {
        return propositionService.listerParCampagne(campagneId);
    }

    @GetMapping("/moi/propositions")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    public List<Proposition> mesPropositions(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return propositionService.listerParFournisseur(utilisateur.getId());
    }

    // choisit la moins-disante d'une campagne clôturée et l'attribue
    @PostMapping("/campagnes/{campagneId}/attribution")
    @PreAuthorize("hasRole('ADMIN')")
    public Proposition attribuer(@PathVariable String campagneId) {
        return propositionService.retenirMeilleure(campagneId);
    }
}
