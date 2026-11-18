package fr.limoges.valadon.gestionfioul.proposition;

import java.math.BigDecimal;

/**
 * Tranche de volume avec son prix au litre.
 *
 * <p>Objet embarqué dans une {@link Proposition} : pas de {@code @Document} ni d'id.
 * {@code volumeMin} est inclus, {@code volumeMax} exclu ({@code null} = pas de limite haute),
 * le prix est en euros.</p>
 */
public class Palier {

    private int volumeMin;          // litres, borne incluse
    private Integer volumeMax;      // litres, borne exclue ; null = pas de limite haute
    private BigDecimal prixLitre;   // en euros

    public Palier() {
    }

    /**
     * Crée un palier.
     *
     * @param volumeMin début de la tranche, en litres (inclus)
     * @param volumeMax fin de la tranche, en litres (exclue) ; {@code null} = pas de limite haute
     * @param prixLitre prix au litre, en euros
     */
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
