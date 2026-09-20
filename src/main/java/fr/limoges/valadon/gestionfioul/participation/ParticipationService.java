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

@Service
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final CampagneService campagneService;
    private final UserService userService;

    public ParticipationService(ParticipationRepository participationRepository,
                                CampagneService campagneService,
                                UserService userService) {
        this.participationRepository = participationRepository;
        this.campagneService = campagneService;
        this.userService = userService;
    }

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

    public Participation trouverParId(String id) {
        return participationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Participation introuvable : " + id));
    }

    public List<Participation> listerParCampagne(String campagneId) {
        return participationRepository.findByCampagneId(campagneId);
    }

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
