package fr.limoges.valadon.gestionfioul.campagne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Vérifie que CampagneScheduler tourne réellement en tâche de fond, sans passer par l'API :
 * une campagne échue est clôturée toute seule, une campagne pas encore échue ne l'est pas.
 * L'intervalle est réduit à 200 ms pour que le test ne dure que quelques secondes.
 */
@SpringBootTest(properties = "gestionfioul.campagnes.cloture-automatique.intervalle-ms=200")
@Testcontainers(disabledWithoutDocker = true)
class CampagneSchedulerTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0.9");

    @Autowired
    private CampagneRepository campagneRepository;

    @BeforeEach
    void viderLesCampagnes() {
        campagneRepository.deleteAll();
    }

    private Campagne uneCampagne(StatutCampagne statut, LocalDateTime dateCloture) {
        Campagne campagne = new Campagne();
        campagne.setLibelle("Campagne de test du scheduler");
        campagne.setDateOuverture(LocalDateTime.now().minusDays(1));
        campagne.setDateCloture(dateCloture);
        campagne.setStatut(statut);
        return campagneRepository.save(campagne);
    }

    @Test
    @DisplayName("une campagne OUVERTE dont la date limite est dépassée est clôturée automatiquement")
    void campagne_echue_cloturee_automatiquement() {
        Campagne echue = uneCampagne(StatutCampagne.OUVERTE, LocalDateTime.now().minusMinutes(1));

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(campagneRepository.findById(echue.getId()).orElseThrow().getStatut())
                        .isEqualTo(StatutCampagne.CLOTUREE));
    }

    @Test
    @DisplayName("une campagne OUVERTE pas encore échue n'est pas touchée par le scheduler")
    void campagne_pas_encore_echue_reste_ouverte() {
        Campagne enCours = uneCampagne(StatutCampagne.OUVERTE, LocalDateTime.now().plusDays(1));

        // le scheduler tourne toutes les 200 ms : on le laisse passer plusieurs fois avant de vérifier
        await().pollDelay(Duration.ofMillis(600)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(campagneRepository.findById(enCours.getId()).orElseThrow().getStatut())
                        .isEqualTo(StatutCampagne.OUVERTE));
    }

    @Test
    @DisplayName("une campagne BROUILLON échue n'est pas clôturée, même si sa date est dépassée")
    void campagne_brouillon_echue_non_touchee() {
        Campagne brouillon = uneCampagne(StatutCampagne.BROUILLON, LocalDateTime.now().minusMinutes(1));

        await().pollDelay(Duration.ofMillis(600)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(campagneRepository.findById(brouillon.getId()).orElseThrow().getStatut())
                        .isEqualTo(StatutCampagne.BROUILLON));
    }
}
