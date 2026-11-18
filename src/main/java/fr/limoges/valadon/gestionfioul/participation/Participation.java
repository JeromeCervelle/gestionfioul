package fr.limoges.valadon.gestionfioul.participation;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Engagement d'un client dans une campagne : le volume de fioul qu'il souhaite commander
 * (document MongoDB de la collection {@code participations}).
 *
 * <p>La participation ne contient que les IDENTIFIANTS de la campagne et du client (références, pas de copie).
 * Le volume est en litres ; l'adresse de livraison peut différer de celle du profil.</p>
 */
@Document(collection = "participations")
public class Participation {

    @Id
    private String id;

    @Indexed
    private String campagneId;

    @Indexed
    private String clientId;

    private int volumeSouhaite;             // litres
    private String adresseLivraison;        // peut différer de l'adresse du profil
    private StatutParticipation statut = StatutParticipation.CONFIRMEE;
    private LocalDateTime dateParticipation = LocalDateTime.now();

    public Participation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampagneId() {
        return campagneId;
    }

    public void setCampagneId(String campagneId) {
        this.campagneId = campagneId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getVolumeSouhaite() {
        return volumeSouhaite;
    }

    public void setVolumeSouhaite(int volumeSouhaite) {
        this.volumeSouhaite = volumeSouhaite;
    }

    public String getAdresseLivraison() {
        return adresseLivraison;
    }

    public void setAdresseLivraison(String adresseLivraison) {
        this.adresseLivraison = adresseLivraison;
    }

    public StatutParticipation getStatut() {
        return statut;
    }

    public void setStatut(StatutParticipation statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateParticipation() {
        return dateParticipation;
    }

    public void setDateParticipation(LocalDateTime dateParticipation) {
        this.dateParticipation = dateParticipation;
    }
}
