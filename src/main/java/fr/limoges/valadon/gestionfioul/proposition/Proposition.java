package fr.limoges.valadon.gestionfioul.proposition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Offre d'un fournisseur pour une campagne, sous forme de barème dégressif par paliers de volume
 * (document MongoDB de la collection {@code propositions}, paliers embarqués).
 *
 * <p>{@code validiteJusqua} : date jusqu'à laquelle le fournisseur garantit ses prix ; elle doit dépasser
 * la clôture de la campagne.</p>
 */
@Document(collection = "propositions")
public class Proposition {

    @Id
    private String id;

    @Indexed
    private String campagneId;

    @Indexed
    private String fournisseurId;

    private LocalDateTime dateProposition = LocalDateTime.now();
    private LocalDateTime validiteJusqua;
    private StatutProposition statut = StatutProposition.SOUMISE;
    private List<Palier> paliers = new ArrayList<>();   // embarqués

    public Proposition() {
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

    public String getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(String fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public LocalDateTime getDateProposition() {
        return dateProposition;
    }

    public void setDateProposition(LocalDateTime dateProposition) {
        this.dateProposition = dateProposition;
    }

    public LocalDateTime getValiditeJusqua() {
        return validiteJusqua;
    }

    public void setValiditeJusqua(LocalDateTime validiteJusqua) {
        this.validiteJusqua = validiteJusqua;
    }

    public StatutProposition getStatut() {
        return statut;
    }

    public void setStatut(StatutProposition statut) {
        this.statut = statut;
    }

    public List<Palier> getPaliers() {
        return paliers;
    }

    public void setPaliers(List<Palier> paliers) {
        this.paliers = paliers;
    }
}
