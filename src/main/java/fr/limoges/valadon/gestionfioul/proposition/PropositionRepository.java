package fr.limoges.valadon.gestionfioul.proposition;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PropositionRepository extends MongoRepository<Proposition, String> {

    List<Proposition> findByCampagneId(String campagneId);

    // propositions en concurrence à comparer à la clôture
    List<Proposition> findByCampagneIdAndStatut(String campagneId, StatutProposition statut);

    boolean existsByCampagneIdAndFournisseurIdAndStatut(String campagneId, String fournisseurId, StatutProposition statut);

    List<Proposition> findByFournisseurId(String fournisseurId);
}
