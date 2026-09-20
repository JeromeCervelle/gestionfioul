package fr.limoges.valadon.gestionfioul.proposition;

import java.math.BigDecimal;

/**
 * Tranche de volume avec son prix au litre.
 * Objet embarqué dans une Proposition : pas de @Document ni d'id.
 */
public class Palier {

    private int volumeMin;          // litres, borne incluse
    private Integer volumeMax;      // litres, borne exclue ; null = pas de limite haute
    private BigDecimal prixLitre;   // en euros

    public Palier() {
    }

    public Palier(int volumeMin, Integer volumeMax, BigDecimal prixLitre) {
        this.volumeMin = volumeMin;
        this.volumeMax = volumeMax;
        this.prixLitre = prixLitre;
    }

    public int getVolumeMin() {
        return volumeMin;
    }

    public void setVolumeMin(int volumeMin) {
        this.volumeMin = volumeMin;
    }

    public Integer getVolumeMax() {
        return volumeMax;
    }

    public void setVolumeMax(Integer volumeMax) {
        this.volumeMax = volumeMax;
    }

    public BigDecimal getPrixLitre() {
        return prixLitre;
    }

    public void setPrixLitre(BigDecimal prixLitre) {
        this.prixLitre = prixLitre;
    }
}
