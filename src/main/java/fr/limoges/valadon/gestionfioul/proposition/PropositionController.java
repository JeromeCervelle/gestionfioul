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

/**
 * Routes des propositions et de l'attribution.
 *
 * <p>Le fournisseur est toujours l'utilisateur connecté ({@code @AuthenticationPrincipal}).
 * Les barèmes des concurrents restent confidentiels : seul l'administrateur voit toutes les propositions.</p>
 */
@RestController
@RequestMapping("/api")
public class PropositionController {

    private final PropositionService propositionService;

    public PropositionController(PropositionService propositionService) {
        this.propositionService = propositionService;
    }

    /**
     * {@code POST /api/campagnes/{campagneId}/propositions} (fournisseur) : déposer un barème.
     *
     * @param campagneId  identifiant de la campagne
     * @param request     date de validité et paliers
     * @param utilisateur fournisseur connecté
     * @return la proposition créée (201) ; 400 si le barème ou la validité sont invalides,
     *         409 si la campagne n'est pas ouverte ou si le fournisseur a déjà proposé
     */
    @PostMapping("/campagnes/{campagneId}/propositions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FOURNISSEUR')")
    public Proposition soumettre(@PathVariable String campagneId,
                                 @RequestBody SoumettrePropositionRequest request,
                                 @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return propositionService.soumettre(
                campagneId, utilisateur.getId(), request.validiteJusqua(), request.paliers());
    }

    /**
     * {@code GET /api/campagnes/{campagneId}/propositions} (administrateur).
     *
     * @param campagneId identifiant de la campagne
     * @return les propositions de tous les fournisseurs
     */
    @GetMapping("/campagnes/{campagneId}/propositions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Proposition> listerParCampagne(@PathVariable String campagneId) {
        return propositionService.listerParCampagne(campagneId);
    }

    /**
     * {@code GET /api/moi/propositions} (fournisseur).
     *
     * @param utilisateur fournisseur connecté
     * @return ses propositions, toutes campagnes confondues
     */
    @GetMapping("/moi/propositions")
    @PreAuthorize("hasRole('FOURNISSEUR')")
    public List<Proposition> mesPropositions(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return propositionService.listerParFournisseur(utilisateur.getId());
    }

    /**
     * {@code POST /api/campagnes/{campagneId}/attribution} (administrateur) : choisit la moins-disante
     * d'une campagne clôturée et l'attribue.
     *
     * @param campagneId identifiant de la campagne
     * @return la proposition retenue ; 409 si la campagne n'est pas clôturée, si aucun volume n'est commandé
     *         ou si aucune proposition n'est valable
     */
    @PostMapping("/campagnes/{campagneId}/attribution")
    @PreAuthorize("hasRole('ADMIN')")
    public Proposition attribuer(@PathVariable String campagneId) {
        return propositionService.retenirMeilleure(campagneId);
    }
}
