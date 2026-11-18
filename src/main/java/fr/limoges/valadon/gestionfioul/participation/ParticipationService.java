package fr.limoges.valadon.gestionfioul.participation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import fr.limoges.valadon.gestionfioul.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.campagne.CampagneService;
import fr.limoges.valadon.gestionfioul.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;
import fr.limoges.valadon.gestionfioul.user.UserService;

/**
 * Règles métier des participations.
 *
 * <p>On ne peut participer, ou annuler, que sur une campagne {@code OUVERTE} dont la date limite n'est pas
 * dépassée. Les exceptions sont traduites en HTTP par {@link fr.limoges.valadon.gestionfioul.common.GestionExceptionHandler GestionExceptionHandler}.</p>
 */
@Service
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final CampagneService campagneService;
    private final UserService userService;

    /**
     * Crée le service.
     *
     * @param participationRepository accès aux participations
     * @param campagneService         pour vérifier que la campagne est ouverte
     * @param userService             pour vérifier le rôle du client et lire son adresse
     */
    public ParticipationService(ParticipationRepository participationRepository,
                                CampagneService campagneService,
                                UserService userService) {
        this.participationRepository = participationRepository;
        this.campagneService = campagneService;
        this.userService = userService;
    }

    /**
     * Enregistre l'engagement d'un client.
     *
     * @param campagneId       identifiant de la campagne
     * @param clientId         identifiant du client connecté
     * @param volumeSouhaite   volume en litres, strictement positif
     * @param adresseLivraison adresse de livraison ; vide ou {@code null} : adresse du profil du client
     * @return la participation enregistrée, au statut {@code CONFIRMEE}
     * @throws IllegalArgumentException volume nul ou négatif, utilisateur qui n'est pas un client
     * @throws NoSuchElementException   client ou campagne inexistants
     * @throws IllegalStateException    campagne fermée, ou participation déjà confirmée pour ce client
     */
    public Participation participer(String campagneId, String clientId,
                                    int volumeSouhaite, String adresseLivraison) {
        if (volumeSouhaite <= 0) {
            throw new IllegalArgumentException("Le volume doit être strictement positif");
        }

        User client = userService.trouverParId(clientId);
        if (client.getRole() != Role.CLIENT) {
            throw new IllegalArgumentException("Seul un client peut participer à une campagne");
        }

        verifierCampagneOuverte(campagneService.trouverParId(campagneId));

        if (participationRepository.existsByCampagneIdAndClientIdAndStatut(
                campagneId, clientId, StatutParticipation.CONFIRMEE)) {
            throw new IllegalStateException(
                    "Ce client participe déjà à la campagne : annulez sa participation pour la refaire");
        }

        Participation participation = new Participation();
        participation.setCampagneId(campagneId);
        participation.setClientId(clientId);
        participation.setVolumeSouhaite(volumeSouhaite);
        participation.setAdresseLivraison(
                adresseLivraison == null || adresseLivraison.isBlank() ? client.getAdresse() : adresseLivraison);
        return participationRepository.save(participation);
    }

    /**
     * Annule la participation d'un client, tant que la campagne est ouverte.
     *
     * @param participationId identifiant de la participation
     * @param clientId        identifiant du client connecté
     * @return la participation, au statut {@code ANNULEE}
     * @throws NoSuchElementException   participation inexistante
     * @throws IllegalArgumentException la participation appartient à un autre client
     * @throws IllegalStateException    participation déjà annulée, ou campagne fermée
     */
    public Participation annuler(String participationId, String clientId) {
        Participation participation = trouverParId(participationId);

        if (!participation.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("Cette participation n'appartient pas à ce client");
        }
        if (participation.getStatut() != StatutParticipation.CONFIRMEE) {
            throw new IllegalStateException("Cette participation est déjà annulée");
        }
        verifierCampagneOuverte(campagneService.trouverParId(participation.getCampagneId()));

        participation.setStatut(StatutParticipation.ANNULEE);
        return participationRepository.save(participation);
    }

    /**
     * Cherche une participation par son id.
     *
     * @param id identifiant
     * @return la participation
     * @throws NoSuchElementException si elle n'existe pas
     */
    public Participation trouverParId(String id) {
        return participationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Participation introuvable : " + id));
    }

    /**
     * Toutes les participations d'une campagne.
     *
     * @param campagneId identifiant de la campagne
     * @return les participations, annulées comprises
     */
    public List<Participation> listerParCampagne(String campagneId) {
        return participationRepository.findByCampagneId(campagneId);
    }

    /**
     * Historique d'un client.
     *
     * @param clientId identifiant du client
     * @return ses participations, toutes campagnes confondues
     */
    public List<Participation> listerParClient(String clientId) {
        return participationRepository.findByClientId(clientId);
    }

    // la date est testée aussi : la campagne peut être encore OUVERTE alors que sa date limite est passée
    private void verifierCampagneOuverte(Campagne campagne) {
        if (campagne.getStatut() != StatutCampagne.OUVERTE
                || campagne.getDateCloture().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La campagne n'accepte plus de modifications de participation");
        }
    }
}
