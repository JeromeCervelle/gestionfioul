package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CampagneRepository extends MongoRepository<Campagne, String> {

    List<Campagne> findByStatut(StatutCampagne statut);

    // campagnes ouvertes dont la date limite est dépassée, à clôturer
    List<Campagne> findByStatutAndDateClotureBefore(StatutCampagne statut, LocalDateTime date);
}
