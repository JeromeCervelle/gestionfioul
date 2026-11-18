package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Accès aux campagnes dans MongoDB. Spring Data écrit les requêtes à partir du NOM des méthodes.
 */
public interface CampagneRepository extends MongoRepository<Campagne, String> {

    /**
     * Campagnes d'un statut.
     *
     * @param statut statut recherché
     * @return les campagnes ayant ce statut
     */
    List<Campagne> findByStatut(StatutCampagne statut);

    /**
     * Campagnes d'un statut dont la date limite est antérieure à une date :
     * sert à trouver les campagnes ouvertes à clôturer.
     *
     * @param statut statut recherché
     * @param date   date de comparaison (maintenant)
     * @return les campagnes concernées
     */
    List<Campagne> findByStatutAndDateClotureBefore(StatutCampagne statut, LocalDateTime date);
}
