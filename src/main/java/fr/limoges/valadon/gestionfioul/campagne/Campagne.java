package fr.limoges.valadon.gestionfioul.campagne;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Période de collecte des commandes de fioul des clients (document MongoDB de la collection {@code campagnes}).
 *
 * <ul>
 *   <li>{@code dateCloture} : date limite de participation ;</li>
 *   <li>{@code volumeTotal} : somme des participations confirmées, en litres, calculée à la clôture (0 avant) ;</li>
 *   <li>{@code zoneLivraison} : code postal, secteur... ;</li>
 *   <li>{@code propositionRetenueId} : renseigné une fois le fournisseur choisi.</li>
 * </ul>
 */
@Document(collection = "campagnes")
public class Campagne {

    @Id
    private String id;

    private String libelle;                 // "Commande groupée janvier 2026"
    private LocalDateTime dateOuverture;
    private LocalDateTime dateCloture;      // date limite de participation
    private StatutCampagne statut = StatutCampagne.BROUILLON;
    private int volumeTotal;                // litres, somme des participations confirmées
    private String zoneLivraison;           // code postal, secteur...
    private String propositionRetenueId;    // renseigné une fois le fournisseur choisi

    public Campagne() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(LocalDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public StatutCampagne getStatut() {
        return statut;
    }

    public void setStatut(StatutCampagne statut) {
        this.statut = statut;
    }

    public int getVolumeTotal() {
        return volumeTotal;
    }

    public void setVolumeTotal(int volumeTotal) {
        this.volumeTotal = volumeTotal;
    }

    public String getZoneLivraison() {
        return zoneLivraison;
    }

    public void setZoneLivraison(String zoneLivraison) {
        this.zoneLivraison = zoneLivraison;
    }

    public String getPropositionRetenueId() {
        return propositionRetenueId;
    }

    public void setPropositionRetenueId(String propositionRetenueId) {
        this.propositionRetenueId = propositionRetenueId;
    }
}
