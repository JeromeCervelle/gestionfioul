package fr.limoges.valadon.gestionfioul.participation;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParticipationRepository extends MongoRepository<Participation, String> {

    List<Participation> findByCampagneId(String campagneId);

    // participations à additionner pour obtenir le volumeTotal d'une campagne
    List<Participation> findByCampagneIdAndStatut(String campagneId, StatutParticipation statut);

    boolean existsByCampagneIdAndClientIdAndStatut(String campagneId, String clientId, StatutParticipation statut);

    // historique d'un client, toutes campagnes confondues
    List<Participation> findByClientId(String clientId);
}
