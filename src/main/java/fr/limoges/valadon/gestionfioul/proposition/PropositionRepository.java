package fr.limoges.valadon.gestionfioul.proposition;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Accès aux propositions dans MongoDB. Spring Data écrit les requêtes à partir du NOM des méthodes.
 */
public interface PropositionRepository extends MongoRepository<Proposition, String> {

    /**
     * Toutes les propositions d'une campagne.
     *
     * @param campagneId identifiant de la campagne
     * @return les propositions, quel que soit leur statut
     */
    List<Proposition> findByCampagneId(String campagneId);

    /**
     * Propositions d'une campagne ayant un statut : celles en concurrence à comparer pour l'attribution.
     *
     * @param campagneId identifiant de la campagne
     * @param statut     statut recherché
     * @return les propositions concernées
     */
    List<Proposition> findByCampagneIdAndStatut(String campagneId, StatutProposition statut);

    /**
     * Indique si un fournisseur a déjà une proposition avec ce statut dans la campagne (détection des doublons).
     *
     * @param campagneId    identifiant de la campagne
     * @param fournisseurId identifiant du fournisseur
     * @param statut        statut recherché
     * @return {@code true} si une telle proposition existe
     */
    boolean existsByCampagneIdAndFournisseurIdAndStatut(String campagneId, String fournisseurId, StatutProposition statut);

    /**
     * Propositions d'un fournisseur, toutes campagnes confondues.
     *
     * @param fournisseurId identifiant du fournisseur
     * @return ses propositions
     */
    List<Proposition> findByFournisseurId(String fournisseurId);
}
