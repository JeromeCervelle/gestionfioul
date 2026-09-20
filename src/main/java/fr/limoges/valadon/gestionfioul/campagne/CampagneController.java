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

@RestController
@RequestMapping("/api/campagnes")
public class CampagneController {

    private final CampagneService campagneService;

    public CampagneController(CampagneService campagneService) {
        this.campagneService = campagneService;
    }

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

    // consultation ouverte à tout utilisateur connecté
    @GetMapping
    public List<Campagne> lister(@RequestParam(defaultValue = "OUVERTE") StatutCampagne statut) {
        return campagneService.listerParStatut(statut);
    }

    @GetMapping("/{id}")
    public Campagne trouver(@PathVariable String id) {
        return campagneService.trouverParId(id);
    }

    @PostMapping("/{id}/ouvrir")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne ouvrir(@PathVariable String id) {
        return campagneService.ouvrir(id);
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne cloturer(@PathVariable String id) {
        return campagneService.cloturer(id);
    }

    @PostMapping("/{id}/livrer")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne marquerLivree(@PathVariable String id) {
        return campagneService.marquerLivree(id);
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    public Campagne annuler(@PathVariable String id) {
        return campagneService.annuler(id);
    }
}
