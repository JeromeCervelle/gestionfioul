package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import fr.limoges.valadon.gestionfioul.participation.Participation;
import fr.limoges.valadon.gestionfioul.participation.ParticipationRepository;
import fr.limoges.valadon.gestionfioul.participation.StatutParticipation;

@Service
public class CampagneService {

    private final CampagneRepository campagneRepository;
    private final ParticipationRepository participationRepository;

    public CampagneService(CampagneRepository campagneRepository,
                           ParticipationRepository participationRepository) {
        this.campagneRepository = campagneRepository;
        this.participationRepository = participationRepository;
    }

    public Campagne creer(Campagne campagne) {
        if (campagne.getLibelle() == null || campagne.getLibelle().isBlank()) {
            throw new IllegalArgumentException("Le libellé est obligatoire");
        }
        if (campagne.getDateOuverture() == null || campagne.getDateCloture() == null) {
            throw new IllegalArgumentException("Les dates d'ouverture et de clôture sont obligatoires");
        }
        if (!campagne.getDateCloture().isAfter(campagne.getDateOuverture())) {
            throw new IllegalArgumentException("La clôture doit être postérieure à l'ouverture");
        }

        campagne.setStatut(StatutCampagne.BROUILLON);
        campagne.setVolumeTotal(0);
        campagne.setPropositionRetenueId(null);
        return campagneRepository.save(campagne);
    }

    public Campagne ouvrir(String id) {
        Campagne campagne = trouverParId(id);
        verifierStatut(campagne, StatutCampagne.BROUILLON);
        if (campagne.getDateCloture().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La date de clôture est déjà dépassée");
        }
        campagne.setStatut(StatutCampagne.OUVERTE);
        return campagneRepository.save(campagne);
    }

    /**
     * Fige le volumeTotal, recalculé à partir des participations confirmées.
     */
    public Campagne cloturer(String id) {
        Campagne campagne = trouverParId(id);
        verifierStatut(campagne, StatutCampagne.OUVERTE);

        int volumeTotal = participationRepository
                .findByCampagneIdAndStatut(id, StatutParticipation.CONFIRMEE)
                .stream()
                .mapToInt(Participation::getVolumeSouhaite)
                .sum();

        campagne.setVolumeTotal(volumeTotal);
        campagne.setStatut(StatutCampagne.CLOTUREE);
        return campagneRepository.save(campagne);
    }

    /**
     * Clôture toutes les campagnes ouvertes dont la date limite est passée.
     * Pensé pour être appelé périodiquement (@Scheduled).
     */
    public List<Campagne> cloturerCampagnesEchues() {
        return campagneRepository
                .findByStatutAndDateClotureBefore(StatutCampagne.OUVERTE, LocalDateTime.now())
                .stream()
                .map(c -> cloturer(c.getId()))
                .toList();
    }

    /**
     * Enregistre la proposition choisie. Le choix lui-même (la moins-disante) relève de PropositionService.
     */
    public Campagne attribuer(String id, String propositionRetenueId) {
        Campagne campagne = trouverParId(id);
        verifierStatut(campagne, StatutCampagne.CLOTUREE);
        campagne.setPropositionRetenueId(propositionRetenueId);
        campagne.setStatut(StatutCampagne.ATTRIBUEE);
        return campagneRepository.save(campagne);
    }

    public Campagne marquerLivree(String id) {
        Campagne campagne = trouverParId(id);
        verifierStatut(campagne, StatutCampagne.ATTRIBUEE);
        campagne.setStatut(StatutCampagne.LIVREE);
        return campagneRepository.save(campagne);
    }

    public Campagne annuler(String id) {
        Campagne campagne = trouverParId(id);
        if (campagne.getStatut() == StatutCampagne.LIVREE || campagne.getStatut() == StatutCampagne.ANNULEE) {
            throw new IllegalStateException(
                    "Une campagne " + campagne.getStatut() + " ne peut plus être annulée");
        }
        campagne.setStatut(StatutCampagne.ANNULEE);
        return campagneRepository.save(campagne);
    }

    public Campagne trouverParId(String id) {
        return campagneRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Campagne introuvable : " + id));
    }

    public List<Campagne> listerParStatut(StatutCampagne statut) {
        return campagneRepository.findByStatut(statut);
    }

    private void verifierStatut(Campagne campagne, StatutCampagne attendu) {
        if (campagne.getStatut() != attendu) {
            throw new IllegalStateException(
                    "La campagne est " + campagne.getStatut() + ", elle devrait être " + attendu);
        }
    }
}
