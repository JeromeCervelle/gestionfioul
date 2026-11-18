package fr.limoges.valadon.gestionfioul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'API gestionfioul.
 */
@SpringBootApplication
@EnableScheduling   // active les méthodes @Scheduled (voir campagne.CampagneScheduler)
public class GestionfioulApplication {

	/**
	 * Démarre l'API (Tomcat embarqué, port 9999).
	 *
	 * @param args arguments de la ligne de commande, transmis à Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(GestionfioulApplication.class, args);
	}

}
