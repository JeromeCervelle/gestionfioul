package fr.limoges.valadon.gestionfioul.campagne;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenche périodiquement la clôture des campagnes dont la date limite est dépassée.
 * Sans ce composant, CampagneService.cloturerCampagnesEchues() existe mais n'est appelée par personne :
 * une campagne resterait OUVERTE indéfiniment après sa date limite, tant qu'un admin ne la clôture pas à la main.
 */
@Component
public class CampagneScheduler {

    private static final Logger log = LoggerFactory.getLogger(CampagneScheduler.class);

    private final CampagneService campagneService;

    public CampagneScheduler(CampagneService campagneService) {
        this.campagneService = campagneService;
    }

    /**
     * fixedDelay (et non fixedRate) : le délai est compté à partir de la FIN de l'exécution précédente,
     * pas de son début. Deux exécutions ne peuvent donc jamais se chevaucher, même si la clôture
     * de plusieurs campagnes prend plus de temps que l'intervalle configuré.
     */
    @Scheduled(fixedDelayString = "${gestionfioul.campagnes.cloture-automatique.intervalle-ms:300000}")
    public void cloturerLesCampagnesEchues() {
        List<Campagne> cloturees = campagneService.cloturerCampagnesEchues();
        if (!cloturees.isEmpty()) {
            log.info("Clôture automatique : {} campagne(s) clôturée(s) : {}",
                    cloturees.size(), cloturees.stream().map(Campagne::getId).toList());
        }
    }
}
