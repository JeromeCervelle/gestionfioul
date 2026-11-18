package fr.limoges.valadon.gestionfioul.campagne;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes {@code /api/campagnes} : consultation (tout utilisateur connecté), création et cycle de vie (administrateur).
 *
 * <p>L'attribution ({@code POST /api/campagnes/{id}/attribution}) est dans
 * {@link fr.limoges.valadon.gestionfioul.proposition.PropositionController PropositionController} :
 * elle consiste à choisir une proposition.</p>
 */
@RestController
@RequestMapping("/api/campagnes")
public class CampagneController {

    private final CampagneService campagneService;

    public CampagneController(CampagneService campagneService) {
        this.campagneService = campagneService;
    }

    /**
     * {@code POST /api/campagnes} (administrateur) : crée une campagne au statut {@code BROUILLON}.
     *
     * @param request libellé, dates et zone de livraison
     * @return la campagne créée (201) ; 400 si une donnée est invalide
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne creer(@RequestBody CreerCampagneRequest request) {
        Campagne campagne = new Campagne();
        campagne.setLibelle(request.libelle());
        campagne.setDateOuverture(request.dateOuverture());
        campagne.setDateCloture(request.dateCloture());
        campagne.setZoneLivraison(request.zoneLivraison());
        return campagneService.creer(campagne);
    }

    /**
     * {@code GET /api/campagnes?statut=...} : consultation ouverte à tout utilisateur connecté.
     *
     * @param statut statut recherché, {@code OUVERTE} par défaut
     * @return les campagnes ayant ce statut
     */
    @GetMapping
    public List<Campagne> lister(@RequestParam(defaultValue = "OUVERTE") StatutCampagne statut) {
        return campagneService.listerParStatut(statut);
    }

    /**
     * {@code GET /api/campagnes/{id}}
     *
     * @param id identifiant de la campagne
     * @return la campagne ; 404 si elle n'existe pas
     */
    @GetMapping("/{id}")
    public Campagne trouver(@PathVariable String id) {
        return campagneService.trouverParId(id);
    }

    /**
     * {@code POST /api/campagnes/{id}/ouvrir} (administrateur) : {@code BROUILLON → OUVERTE}.
     *
     * @param id identifiant de la campagne
     * @return la campagne ouverte ; 409 si elle n'est pas en brouillon ou si sa date de clôture est dépassée
     */
    @PostMapping("/{id}/ouvrir")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne ouvrir(@PathVariable String id) {
        return campagneService.ouvrir(id);
    }

    /**
     * {@code POST /api/campagnes/{id}/cloturer} (administrateur) : {@code OUVERTE → CLOTUREE}, volume total figé.
     *
     * @param id identifiant de la campagne
     * @return la campagne clôturée ; 409 si elle n'est pas ouverte
     */
    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne cloturer(@PathVariable String id) {
        return campagneService.cloturer(id);
    }

    /**
     * {@code POST /api/campagnes/{id}/livrer} (administrateur) : {@code ATTRIBUEE → LIVREE}.
     *
     * @param id identifiant de la campagne
     * @return la campagne livrée ; 409 si elle n'est pas attribuée
     */
    @PostMapping("/{id}/livrer")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne marquerLivree(@PathVariable String id) {
        return campagneService.marquerLivree(id);
    }

    /**
     * {@code POST /api/campagnes/{id}/annuler} (administrateur) : annulation définitive.
     *
     * @param id identifiant de la campagne
     * @return la campagne annulée ; 409 si elle est déjà livrée ou annulée
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne annuler(@PathVariable String id) {
        return campagneService.annuler(id);
    }
}
