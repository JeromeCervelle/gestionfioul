package fr.limoges.valadon.gestionfioul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // active les méthodes @Scheduled (voir campagne.CampagneScheduler)
public class GestionfioulApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionfioulApplication.class, args);
	}

}
