package fr.limoges.valadon.gestionfioul.campagne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.limoges.valadon.gestionfioul.participation.Participation;
import fr.limoges.valadon.gestionfioul.participation.ParticipationRepository;
import fr.limoges.valadon.gestionfioul.participation.StatutParticipation;

@ExtendWith(MockitoExtension.class)
class CampagneServiceTest {

    @Mock
    private CampagneRepository campagneRepository;

    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private CampagneService campagneService;

    /** Campagne dont la date limite est dans 7 jours. */
    private Campagne uneCampagne(String id, StatutCampagne statut) {
        Campagne campagne = new Campagne();
        campagne.setId(id);
        campagne.setLibelle("Janvier");
        campagne.setDateOuverture(LocalDateTime.now().minusDays(1));
        campagne.setDateCloture(LocalDateTime.now().plusDays(7));
        campagne.setStatut(statut);
        return campagne;
    }

    private Participation uneParticipation(int volume) {
        Participation participation = new Participation();
        participation.setVolumeSouhaite(volume);
        return participation;
    }

    private void trouvable(Campagne campagne) {
        when(campagneRepository.findById(campagne.getId())).thenReturn(Optional.of(campagne));
    }

    private void enregistrementTelQuel() {
        when(campagneRepository.save(any(Campagne.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- creer

    @Test
    @DisplayName("creer : la campagne démarre en BROUILLON, sans volume ni proposition retenue")
    void creer_force_les_valeurs_initiales() {
        Campagne campagne = uneCampagne(null, StatutCampagne.ATTRIBUEE);
        campagne.setVolumeTotal(999);
        campagne.setPropositionRetenueId("p1");
        enregistrementTelQuel();

        Campagne resultat = campagneService.creer(campagne);

        assertThat(resultat.getStatut()).isEqualTo(StatutCampagne.BROUILLON);
        assertThat(resultat.getVolumeTotal()).isZero();
        assertThat(resultat.getPropositionRetenueId()).isNull();
    }

    @Test
    @DisplayName("creer : libellé absent ou vide refusé")
    void creer_libelle_absent_refuse() {
        Campagne sansLibelle = uneCampagne(null, StatutCampagne.BROUILLON);
        sansLibelle.setLibelle(null);
        Campagne libelleVide = uneCampagne(null, StatutCampagne.BROUILLON);
        libelleVide.setLibelle("   ");

        assertThatThrownBy(() -> campagneService.creer(sansLibelle)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> campagneService.creer(libelleVide)).isInstanceOf(IllegalArgumentException.class);
        verify(campagneRepository, never()).save(any());
    }

    @Test
    @DisplayName("creer : dates d'ouverture et de clôture obligatoires")
    void creer_dates_absentes_refusees() {
        Campagne sansOuverture = uneCampagne(null, StatutCampagne.BROUILLON);
        sansOuverture.setDateOuverture(null);
        Campagne sansCloture = uneCampagne(null, StatutCampagne.BROUILLON);
        sansCloture.setDateCloture(null);

        assertThatThrownBy(() -> campagneService.creer(sansOuverture)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> campagneService.creer(sansCloture)).isInstanceOf(IllegalArgumentException.class);
        verify(campagneRepository, never()).save(any());
    }

    @Test
    @DisplayName("creer : la clôture doit être strictement postérieure à l'ouverture")
    void creer_cloture_pas_apres_ouverture_refusee() {
        Campagne memeInstant = uneCampagne(null, StatutCampagne.BROUILLON);
        memeInstant.setDateCloture(memeInstant.getDateOuverture());
        Campagne avant = uneCampagne(null, StatutCampagne.BROUILLON);
        avant.setDateCloture(avant.getDateOuverture().minusHours(1));

        assertThatThrownBy(() -> campagneService.creer(memeInstant)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> campagneService.creer(avant)).isInstanceOf(IllegalArgumentException.class);
        verify(campagneRepository, never()).save(any());
    }

    // --------------------------------------------------------------- ouvrir

    @Test
    @DisplayName("ouvrir : BROUILLON -> OUVERTE")
    void ouvrir_passe_en_ouverte() {
        Campagne campagne = uneCampagne("c1", StatutCampagne.BROUILLON);
        trouvable(campagne);
        enregistrementTelQuel();

        assertThat(campagneService.ouvrir("c1").getStatut()).isEqualTo(StatutCampagne.OUVERTE);
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "BROUILLON", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("ouvrir : refusé si la campagne n'est pas en BROUILLON")
    void ouvrir_refuse_hors_brouillon(StatutCampagne statut) {
        trouvable(uneCampagne("c1", statut));

        assertThatThrownBy(() -> campagneService.ouvrir("c1")).isInstanceOf(IllegalStateException.class);
        verify(campagneRepository, never()).save(any());
    }

    @Test
    @DisplayName("ouvrir : refusé si la date de clôture est déjà passée")
    void ouvrir_refuse_si_cloture_depassee() {
        Campagne campagne = uneCampagne("c1", StatutCampagne.BROUILLON);
        campagne.setDateCloture(LocalDateTime.now().minusMinutes(1));
        trouvable(campagne);

        assertThatThrownBy(() -> campagneService.ouvrir("c1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dépassée");
        verify(campagneRepository, never()).save(any());
    }

    @Test
    @DisplayName("ouvrir : campagne inexistante")
    void ouvrir_campagne_inexistante() {
        when(campagneRepository.findById("c1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campagneService.ouvrir("c1")).isInstanceOf(NoSuchElementException.class);
    }

    // ------------------------------------------------------------- cloturer

    @Test
    @DisplayName("cloturer : fige le volumeTotal avec la somme des participations confirmées")
    void cloturer_fige_le_volume_total() {
        Campagne campagne = uneCampagne("c1", StatutCampagne.OUVERTE);
        trouvable(campagne);
        when(participationRepository.findByCampagneIdAndStatut("c1", StatutParticipation.CONFIRMEE))
                .thenReturn(List.of(uneParticipation(2000), uneParticipation(3500)));
        enregistrementTelQuel();

        Campagne resultat = campagneService.cloturer("c1");

        assertThat(resultat.getStatut()).isEqualTo(StatutCampagne.CLOTUREE);
        assertThat(resultat.getVolumeTotal()).isEqualTo(5500);
    }

    @Test
    @DisplayName("cloturer : sans participation, le volumeTotal vaut 0")
    void cloturer_sans_participation() {
        Campagne campagne = uneCampagne("c1", StatutCampagne.OUVERTE);
        campagne.setVolumeTotal(123);
        trouvable(campagne);
        when(participationRepository.findByCampagneIdAndStatut("c1", StatutParticipation.CONFIRMEE))
                .thenReturn(List.of());
        enregistrementTelQuel();

        assertThat(campagneService.cloturer("c1").getVolumeTotal()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "OUVERTE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("cloturer : refusé si la campagne n'est pas OUVERTE")
    void cloturer_refuse_hors_ouverte(StatutCampagne statut) {
        trouvable(uneCampagne("c1", statut));

        assertThatThrownBy(() -> campagneService.cloturer("c1")).isInstanceOf(IllegalStateException.class);
        verify(campagneRepository, never()).save(any());
    }

    // ----------------------------------------------- cloturerCampagnesEchues

    @Test
    @DisplayName("cloturerCampagnesEchues : clôture chaque campagne ouverte dont la date est passée")
    void cloturerCampagnesEchues_cloture_toutes_les_campagnes_echues() {
        Campagne c1 = uneCampagne("c1", StatutCampagne.OUVERTE);
        Campagne c2 = uneCampagne("c2", StatutCampagne.OUVERTE);
        when(campagneRepository.findByStatutAndDateClotureBefore(eq(StatutCampagne.OUVERTE), any(LocalDateTime.class)))
                .thenReturn(List.of(c1, c2));
        trouvable(c1);
        trouvable(c2);
        enregistrementTelQuel();

        List<Campagne> resultat = campagneService.cloturerCampagnesEchues();

        assertThat(resultat).hasSize(2)
                .allSatisfy(c -> assertThat(c.getStatut()).isEqualTo(StatutCampagne.CLOTUREE));
    }

    @Test
    @DisplayName("cloturerCampagnesEchues : rien à faire s'il n'y a aucune campagne échue")
    void cloturerCampagnesEchues_aucune() {
        when(campagneRepository.findByStatutAndDateClotureBefore(eq(StatutCampagne.OUVERTE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertThat(campagneService.cloturerCampagnesEchues()).isEmpty();
        verify(campagneRepository, never()).save(any());
    }

    // ------------------------------------------------------------- attribuer

    @Test
    @DisplayName("attribuer : CLOTUREE -> ATTRIBUEE avec la proposition retenue")
    void attribuer_enregistre_la_proposition_retenue() {
        trouvable(uneCampagne("c1", StatutCampagne.CLOTUREE));
        enregistrementTelQuel();

        Campagne resultat = campagneService.attribuer("c1", "p1");

        assertThat(resultat.getStatut()).isEqualTo(StatutCampagne.ATTRIBUEE);
        assertThat(resultat.getPropositionRetenueId()).isEqualTo("p1");
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "CLOTUREE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("attribuer : refusé si la campagne n'est pas CLOTUREE")
    void attribuer_refuse_hors_cloturee(StatutCampagne statut) {
        Campagne campagne = uneCampagne("c1", statut);
        trouvable(campagne);

        assertThatThrownBy(() -> campagneService.attribuer("c1", "p1")).isInstanceOf(IllegalStateException.class);
        assertThat(campagne.getPropositionRetenueId()).isNull();
        verify(campagneRepository, never()).save(any());
    }

    // -------------------------------------------------------- marquerLivree

    @Test
    @DisplayName("marquerLivree : ATTRIBUEE -> LIVREE")
    void marquerLivree_passe_en_livree() {
        trouvable(uneCampagne("c1", StatutCampagne.ATTRIBUEE));
        enregistrementTelQuel();

        assertThat(campagneService.marquerLivree("c1").getStatut()).isEqualTo(StatutCampagne.LIVREE);
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "ATTRIBUEE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("marquerLivree : refusé si la campagne n'est pas ATTRIBUEE")
    void marquerLivree_refuse_hors_attribuee(StatutCampagne statut) {
        trouvable(uneCampagne("c1", statut));

        assertThatThrownBy(() -> campagneService.marquerLivree("c1")).isInstanceOf(IllegalStateException.class);
        verify(campagneRepository, never()).save(any());
    }

    // --------------------------------------------------------------- annuler

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = {"LIVREE", "ANNULEE"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("annuler : possible tant que la campagne n'est ni LIVREE ni déjà ANNULEE")
    void annuler_possible(StatutCampagne statut) {
        trouvable(uneCampagne("c1", statut));
        enregistrementTelQuel();

        assertThat(campagneService.annuler("c1").getStatut()).isEqualTo(StatutCampagne.ANNULEE);
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = {"LIVREE", "ANNULEE"})
    @DisplayName("annuler : refusé si la campagne est LIVREE ou déjà ANNULEE")
    void annuler_refuse(StatutCampagne statut) {
        Campagne campagne = uneCampagne("c1", statut);
        trouvable(campagne);

        assertThatThrownBy(() -> campagneService.annuler("c1")).isInstanceOf(IllegalStateException.class);
        assertThat(campagne.getStatut()).isEqualTo(statut);
        verify(campagneRepository, never()).save(any());
    }

    // ------------------------------------------------------------ consulter

    @Test
    @DisplayName("trouverParId : campagne inexistante")
    void trouverParId_inexistante() {
        when(campagneRepository.findById("c1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campagneService.trouverParId("c1"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("listerParStatut : délègue au repository")
    void listerParStatut_delegue() {
        List<Campagne> ouvertes = List.of(uneCampagne("c1", StatutCampagne.OUVERTE));
        when(campagneRepository.findByStatut(StatutCampagne.OUVERTE)).thenReturn(ouvertes);

        assertThat(campagneService.listerParStatut(StatutCampagne.OUVERTE)).isEqualTo(ouvertes);
    }
}
