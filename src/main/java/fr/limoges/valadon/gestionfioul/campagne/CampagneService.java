package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import fr.limoges.valadon.gestionfioul.participation.Participation;
import fr.limoges.valadon.gestionfioul.participation.ParticipationRepository;
import fr.limoges.valadon.gestionfioul.participation.StatutParticipation;

/**
 * Règles métier des campagnes : création et transitions du cycle de vie.
 *
 * <p>Une transition demandée depuis un mauvais statut lève une {@link IllegalStateException} (409) ;
 * les exceptions sont traduites en HTTP par {@link fr.limoges.valadon.gestionfioul.common.GestionExceptionHandler GestionExceptionHandler}.</p>
 */
@Service
public class CampagneService {

    private final CampagneRepository campagneRepository;
    private final ParticipationRepository participationRepository;

    /**
     * Crée le service.
     *
     * @param campagneRepository      accès aux campagnes
     * @param participationRepository accès aux participations, pour calculer le volume total à la clôture
     */
    public CampagneService(CampagneRepository campagneRepository,
                           ParticipationRepository participationRepository) {
        this.campagneRepository = campagneRepository;
        this.participationRepository = participationRepository;
    }

    /**
     * Crée une campagne au statut {@code BROUILLON}, volume total à 0, sans proposition retenue.
     *
     * @param campagne libellé, dates et zone de livraison
     * @return la campagne enregistrée, avec son id
     * @throws IllegalArgumentException libellé ou date absents, clôture non postérieure à l'ouverture
     */
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

    /**
     * {@code BROUILLON → OUVERTE} : clients et fournisseurs peuvent participer.
     *
     * @param id identifiant de la campagne
     * @return la campagne ouverte
     * @throws NoSuchElementException    campagne inexistante
     * @throws IllegalStateException     campagne pas en brouillon, ou date de clôture déjà dépassée
     */
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
     * {@code OUVERTE → CLOTUREE} : fige le {@code volumeTotal}, recalculé à partir des participations confirmées.
     *
     * @param id identifiant de la campagne
     * @return la campagne clôturée
     * @throws NoSuchElementException campagne inexistante
     * @throws IllegalStateException  campagne pas ouverte
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
     *
     * <p>Appelé périodiquement par {@link CampagneScheduler}.</p>
     *
     * @return les campagnes clôturées (liste vide s'il n'y en avait aucune)
     */
    public List<Campagne> cloturerCampagnesEchues() {
        return campagneRepository
                .findByStatutAndDateClotureBefore(StatutCampagne.OUVERTE, LocalDateTime.now())
                .stream()
                .map(c -> cloturer(c.getId()))
                .toList();
    }

    /**
     * {@code CLOTUREE → ATTRIBUEE} : enregistre la proposition choisie.
     *
     * <p>Le choix lui-même (la moins-disante) relève de
     * {@link fr.limoges.valadon.gestionfioul.proposition.PropositionService#retenirMeilleure(String) PropositionService.retenirMeilleure}.</p>
     *
     * @param id                   identifiant de la campagne
     * @param propositionRetenueId identifiant de la proposition gagnante
     * @return la campagne attribuée
     * @throws NoSuchElementException campagne inexistante
     * @throws IllegalStateException  campagne pas clôturée
     */
    public Campagne attribuer(String id, String propositionRetenueId) {
        Campagne campagne = trouverParId(id);
        verifierStatut(campagne, StatutCampagne.CLOTUREE);
        campagne.setPropositionRetenueId(propositionRetenueId);
        campagne.setStatut(StatutCampagne.ATTRIBUEE);
        return campagneRepository.save(campagne);
    }

    /**
     * {@code ATTRIBUEE → LIVREE}.
     *
     * @param id identifiant de la campagne
     * @return la campagne livrée
     * @throws NoSuchElementException campagne inexistante
     * @throws IllegalStateException  campagne pas attribuée
     */
    public Campagne marquerLivree(String id) {
        Campagne campagne = trouverParId(id);
        verifierStatut(campagne, StatutCampagne.ATTRIBUEE);
        campagne.setStatut(StatutCampagne.LIVREE);
        return campagneRepository.save(campagne);
    }

    /**
     * Annule la campagne, quel que soit son statut, sauf si elle est déjà livrée ou annulée.
     *
     * @param id identifiant de la campagne
     * @return la campagne annulée
     * @throws NoSuchElementException campagne inexistante
     * @throws IllegalStateException  campagne livrée ou déjà annulée
     */
    public Campagne annuler(String id) {
        Campagne campagne = trouverParId(id);
        if (campagne.getStatut() == StatutCampagne.LIVREE || campagne.getStatut() == StatutCampagne.ANNULEE) {
            throw new IllegalStateException(
                    "Une campagne " + campagne.getStatut() + " ne peut plus être annulée");
        }
        campagne.setStatut(StatutCampagne.ANNULEE);
        return campagneRepository.save(campagne);
    }

    /**
     * Cherche une campagne par son id.
     *
     * @param id identifiant
     * @return la campagne
     * @throws NoSuchElementException si elle n'existe pas
     */
    public Campagne trouverParId(String id) {
        return campagneRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Campagne introuvable : " + id));
    }

    /**
     * Campagnes d'un statut.
     *
     * @param statut statut recherché
     * @return les campagnes ayant ce statut
     */
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
