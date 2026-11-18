package fr.limoges.valadon.gestionfioul.participation;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Accès aux participations dans MongoDB. Spring Data écrit les requêtes à partir du NOM des méthodes.
 */
public interface ParticipationRepository extends MongoRepository<Participation, String> {

    /**
     * Toutes les participations d'une campagne.
     *
     * @param campagneId identifiant de la campagne
     * @return les participations, annulées comprises
     */
    List<Participation> findByCampagneId(String campagneId);

    /**
     * Participations d'une campagne ayant un statut : celles à additionner pour obtenir le {@code volumeTotal}.
     *
     * @param campagneId identifiant de la campagne
     * @param statut     statut recherché
     * @return les participations concernées
     */
    List<Participation> findByCampagneIdAndStatut(String campagneId, StatutParticipation statut);

    /**
     * Indique si un client a déjà une participation avec ce statut dans la campagne (détection des doublons).
     *
     * @param campagneId identifiant de la campagne
     * @param clientId   identifiant du client
     * @param statut     statut recherché
     * @return {@code true} si une telle participation existe
     */
    boolean existsByCampagneIdAndClientIdAndStatut(String campagneId, String clientId, StatutParticipation statut);

    /**
     * Historique d'un client, toutes campagnes confondues.
     *
     * @param clientId identifiant du client
     * @return ses participations
     */
    List<Participation> findByClientId(String clientId);
}
