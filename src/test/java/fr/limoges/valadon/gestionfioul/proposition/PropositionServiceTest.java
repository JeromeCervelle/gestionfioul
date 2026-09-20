package fr.limoges.valadon.gestionfioul.proposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.limoges.valadon.gestionfioul.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.campagne.CampagneService;
import fr.limoges.valadon.gestionfioul.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;
import fr.limoges.valadon.gestionfioul.user.UserService;

@ExtendWith(MockitoExtension.class)
class PropositionServiceTest {

    @Mock
    private PropositionRepository propositionRepository;

    @Mock
    private CampagneService campagneService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PropositionService propositionService;

    // ----------------------------------------------------------------- outils

    private static Palier palier(int min, Integer max, String prix) {
        return new Palier(min, max, prix == null ? null : new BigDecimal(prix));
    }

    /** Barème valide : 1,10 € jusqu'à 5000 L, 1,05 € de 5000 à 15000 L, 1,00 € au-delà. */
    private List<Palier> baremeValide() {
        return List.of(palier(0, 5000, "1.10"), palier(5000, 15000, "1.05"), palier(15000, null, "1.00"));
    }

    private User unUtilisateur(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Campagne uneCampagne(String id, StatutCampagne statut, LocalDateTime dateCloture) {
        Campagne campagne = new Campagne();
        campagne.setId(id);
        campagne.setStatut(statut);
        campagne.setDateCloture(dateCloture);
        return campagne;
    }

    private Campagne uneCampagneCloturee(int volumeTotal) {
        Campagne campagne = uneCampagne("c1", StatutCampagne.CLOTUREE, LocalDateTime.now().minusDays(1));
        campagne.setVolumeTotal(volumeTotal);
        return campagne;
    }

    private Proposition uneProposition(String id, LocalDateTime dateProposition, LocalDateTime validiteJusqua,
                                       List<Palier> paliers) {
        Proposition proposition = new Proposition();
        proposition.setId(id);
        proposition.setCampagneId("c1");
        proposition.setFournisseurId("f-" + id);
        proposition.setDateProposition(dateProposition);
        proposition.setValiditeJusqua(validiteJusqua);
        proposition.setPaliers(paliers);
        return proposition;
    }

    /** Proposition valable encore une semaine, avec un prix unique quel que soit le volume. */
    private Proposition unPrixUnique(String id, String prix, LocalDateTime dateProposition) {
        return uneProposition(id, dateProposition, LocalDateTime.now().plusDays(7), List.of(palier(0, null, prix)));
    }

    // ---------------------------------------------------------------- soumettre

    /** Fournisseur valide et campagne ouverte, clôturant dans 7 jours. */
    private Campagne contexteDeSoumissionValide() {
        Campagne campagne = uneCampagne("c1", StatutCampagne.OUVERTE, LocalDateTime.now().plusDays(7));
        when(userService.trouverParId("f1")).thenReturn(unUtilisateur("f1", Role.FOURNISSEUR));
        when(campagneService.trouverParId("c1")).thenReturn(campagne);
        return campagne;
    }

    @Test
    @DisplayName("soumettre : enregistre une proposition SOUMISE avec ses paliers triés")
    void soumettre_enregistre_une_proposition_avec_paliers_tries() {
        Campagne campagne = contexteDeSoumissionValide();
        when(propositionRepository.save(any(Proposition.class))).thenAnswer(i -> i.getArgument(0));
        LocalDateTime validite = campagne.getDateCloture().plusDays(1);
        List<Palier> dansLeDesordre = List.of(palier(5000, null, "1.05"), palier(0, 5000, "1.10"));

        Proposition resultat = propositionService.soumettre("c1", "f1", validite, dansLeDesordre);

        assertThat(resultat.getCampagneId()).isEqualTo("c1");
        assertThat(resultat.getFournisseurId()).isEqualTo("f1");
        assertThat(resultat.getStatut()).isEqualTo(StatutProposition.SOUMISE);
        assertThat(resultat.getValiditeJusqua()).isEqualTo(validite);
        assertThat(resultat.getPaliers()).extracting(Palier::getVolumeMin).containsExactly(0, 5000);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "FOURNISSEUR", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("soumettre : seul un fournisseur peut soumettre")
    void soumettre_refuse_un_non_fournisseur(Role role) {
        when(userService.trouverParId("f1")).thenReturn(unUtilisateur("f1", role));

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1", LocalDateTime.now().plusDays(9), baremeValide()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(propositionRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "OUVERTE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("soumettre : refusé si la campagne n'est pas OUVERTE")
    void soumettre_refuse_si_campagne_pas_ouverte(StatutCampagne statut) {
        when(userService.trouverParId("f1")).thenReturn(unUtilisateur("f1", Role.FOURNISSEUR));
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", statut, LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1", LocalDateTime.now().plusDays(9), baremeValide()))
                .isInstanceOf(IllegalStateException.class);
        verify(propositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("soumettre : refusé si la date limite de la campagne est passée")
    void soumettre_refuse_si_date_limite_depassee() {
        when(userService.trouverParId("f1")).thenReturn(unUtilisateur("f1", Role.FOURNISSEUR));
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", StatutCampagne.OUVERTE, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1", LocalDateTime.now().plusDays(9), baremeValide()))
                .isInstanceOf(IllegalStateException.class);
        verify(propositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("soumettre : la validité doit être renseignée")
    void soumettre_refuse_une_validite_absente() {
        contexteDeSoumissionValide();

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1", null, baremeValide()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valable");
        verify(propositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("soumettre : la validité doit dépasser strictement la clôture de la campagne")
    void soumettre_refuse_une_validite_qui_ne_depasse_pas_la_cloture() {
        Campagne campagne = contexteDeSoumissionValide();

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1", campagne.getDateCloture(), baremeValide()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1",
                campagne.getDateCloture().minusHours(1), baremeValide()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(propositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("soumettre : un fournisseur ne peut avoir qu'une proposition SOUMISE par campagne")
    void soumettre_refuse_un_doublon() {
        Campagne campagne = contexteDeSoumissionValide();
        when(propositionRepository.existsByCampagneIdAndFournisseurIdAndStatut("c1", "f1", StatutProposition.SOUMISE))
                .thenReturn(true);

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1",
                campagne.getDateCloture().plusDays(1), baremeValide()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà");
        verify(propositionRepository, never()).save(any());
    }

    static Stream<Arguments> baremesInvalides() {
        return Stream.of(
                arguments("liste absente", null),
                arguments("liste vide", List.of()),
                arguments("premier palier qui ne commence pas à 0",
                        List.of(palier(100, null, "1.10"))),
                arguments("trou entre deux paliers",
                        List.of(palier(0, 5000, "1.10"), palier(6000, null, "1.05"))),
                arguments("chevauchement de deux paliers",
                        List.of(palier(0, 5000, "1.10"), palier(4000, null, "1.05"))),
                arguments("dernier palier avec une limite haute",
                        List.of(palier(0, 5000, "1.10"), palier(5000, 10000, "1.05"))),
                arguments("palier intermédiaire sans limite haute",
                        List.of(palier(0, null, "1.10"), palier(5000, null, "1.05"))),
                arguments("volume max inférieur ou égal au volume min",
                        List.of(palier(0, 0, "1.10"), palier(0, null, "1.05"))),
                arguments("prix absent", List.of(palier(0, null, null))),
                arguments("prix nul", List.of(palier(0, null, "0"))),
                arguments("prix négatif", List.of(palier(0, null, "-1.05"))));
    }

    @ParameterizedTest(name = "barème refusé : {0}")
    @MethodSource("baremesInvalides")
    @DisplayName("soumettre : un barème incohérent est refusé")
    void soumettre_refuse_un_bareme_invalide(String cas, List<Palier> paliers) {
        Campagne campagne = contexteDeSoumissionValide();

        assertThatThrownBy(() -> propositionService.soumettre("c1", "f1",
                campagne.getDateCloture().plusDays(1), paliers))
                .isInstanceOf(IllegalArgumentException.class);
        verify(propositionRepository, never()).save(any());
    }

    @Test
    @DisplayName("soumettre : un palier unique sans limite est un barème valide")
    void soumettre_accepte_un_palier_unique() {
        Campagne campagne = contexteDeSoumissionValide();
        when(propositionRepository.save(any(Proposition.class))).thenAnswer(i -> i.getArgument(0));

        Proposition resultat = propositionService.soumettre("c1", "f1",
                campagne.getDateCloture().plusDays(1), List.of(palier(0, null, "1.08")));

        assertThat(resultat.getPaliers()).hasSize(1);
    }

    // ------------------------------------------------------------------ prixPour

    @ParameterizedTest(name = "{0} L -> {1} €/L")
    @CsvSource({"0,1.10", "4999,1.10", "5000,1.05", "14999,1.05", "15000,1.00", "100000,1.00"})
    @DisplayName("prixPour : renvoie le prix du palier qui contient le volume (min inclus, max exclu)")
    void prixPour_choisit_le_bon_palier(int volume, String prixAttendu) {
        Proposition proposition = uneProposition("p1", LocalDateTime.now(), LocalDateTime.now().plusDays(7), baremeValide());

        assertThat(propositionService.prixPour(proposition, volume)).hasValueSatisfying(
                prix -> assertThat(prix).isEqualByComparingTo(prixAttendu));
    }

    @Test
    @DisplayName("prixPour : vide si aucun palier ne couvre le volume")
    void prixPour_vide_si_volume_non_couvert() {
        Proposition proposition = uneProposition("p1", LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                List.of(palier(1000, null, "1.05")));

        assertThat(propositionService.prixPour(proposition, 500)).isEmpty();
    }

    @Test
    @DisplayName("prixPour : vide si la proposition n'a aucun palier")
    void prixPour_vide_sans_palier() {
        Proposition proposition = uneProposition("p1", LocalDateTime.now(), LocalDateTime.now().plusDays(7), List.of());

        assertThat(propositionService.prixPour(proposition, 1000)).isEmpty();
    }

    // ---------------------------------------------------------- retenirMeilleure

    @ParameterizedTest(name = "volume de {0} L -> gagnante : {1}")
    @CsvSource({"8000,A", "3000,B"})
    @DisplayName("retenirMeilleure : retient la moins-disante au volumeTotal de la campagne")
    void retenirMeilleure_retient_la_moins_disante_au_volume_total(int volumeTotal, String gagnanteAttendue) {
        LocalDateTime maintenant = LocalDateTime.now();
        // A : 1,10 € jusqu'à 5000 L puis 1,00 € ; B : 1,05 € quel que soit le volume
        Proposition a = uneProposition("A", maintenant.minusHours(2), maintenant.plusDays(7),
                List.of(palier(0, 5000, "1.10"), palier(5000, null, "1.00")));
        Proposition b = unPrixUnique("B", "1.05", maintenant.minusHours(1));
        List<Proposition> soumises = List.of(a, b);
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(volumeTotal));
        when(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE)).thenReturn(soumises);

        Proposition gagnante = propositionService.retenirMeilleure("c1");

        assertThat(gagnante.getId()).isEqualTo(gagnanteAttendue);
        assertThat(gagnante.getStatut()).isEqualTo(StatutProposition.RETENUE);
        Proposition perdante = gagnante == a ? b : a;
        assertThat(perdante.getStatut()).isEqualTo(StatutProposition.REJETEE);
        verify(propositionRepository).saveAll(soumises);
        verify(campagneService).attribuer("c1", gagnanteAttendue);
    }

    @Test
    @DisplayName("retenirMeilleure : à prix égal, la proposition soumise en premier l'emporte")
    void retenirMeilleure_departage_a_prix_egal_par_la_date_de_soumission() {
        LocalDateTime maintenant = LocalDateTime.now();
        Proposition tardive = unPrixUnique("tardive", "1.05", maintenant.minusHours(1));
        Proposition precoce = unPrixUnique("precoce", "1.05", maintenant.minusDays(1));
        // la plus tardive est en tête de liste : le départage ne doit pas dépendre de l'ordre
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(8000));
        when(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE))
                .thenReturn(List.of(tardive, precoce));

        Proposition gagnante = propositionService.retenirMeilleure("c1");

        assertThat(gagnante).isSameAs(precoce);
        assertThat(tardive.getStatut()).isEqualTo(StatutProposition.REJETEE);
    }

    @Test
    @DisplayName("retenirMeilleure : ignore une proposition expirée, même moins chère, et la rejette")
    void retenirMeilleure_ignore_les_propositions_expirees() {
        LocalDateTime maintenant = LocalDateTime.now();
        Proposition expiree = uneProposition("expiree", maintenant.minusDays(3), maintenant.minusHours(1),
                List.of(palier(0, null, "0.90")));
        Proposition valable = unPrixUnique("valable", "1.05", maintenant.minusDays(2));
        List<Proposition> soumises = List.of(expiree, valable);
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(8000));
        when(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE)).thenReturn(soumises);

        Proposition gagnante = propositionService.retenirMeilleure("c1");

        assertThat(gagnante).isSameAs(valable);
        assertThat(expiree.getStatut()).isEqualTo(StatutProposition.REJETEE);
        verify(propositionRepository).saveAll(soumises);
    }

    @Test
    @DisplayName("retenirMeilleure : ignore une proposition dont les paliers ne couvrent pas le volume")
    void retenirMeilleure_ignore_les_propositions_qui_ne_couvrent_pas_le_volume() {
        LocalDateTime maintenant = LocalDateTime.now();
        Proposition nonCouvrante = uneProposition("nonCouvrante", maintenant.minusDays(2), maintenant.plusDays(7),
                List.of(palier(10000, null, "0.90")));
        Proposition couvrante = unPrixUnique("couvrante", "1.05", maintenant.minusDays(1));
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(3000));
        when(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE))
                .thenReturn(List.of(nonCouvrante, couvrante));

        assertThat(propositionService.retenirMeilleure("c1")).isSameAs(couvrante);
    }

    @Test
    @DisplayName("retenirMeilleure : refusé s'il ne reste aucune proposition valable")
    void retenirMeilleure_refuse_sans_proposition_valable() {
        LocalDateTime maintenant = LocalDateTime.now();
        Proposition expiree = uneProposition("expiree", maintenant.minusDays(3), maintenant.minusHours(1),
                List.of(palier(0, null, "0.90")));
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(8000));
        when(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE))
                .thenReturn(List.of(expiree));

        assertThatThrownBy(() -> propositionService.retenirMeilleure("c1"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(expiree.getStatut()).isEqualTo(StatutProposition.SOUMISE);
        verify(propositionRepository, never()).saveAll(any());
        verify(campagneService, never()).attribuer(any(), any());
    }

    @Test
    @DisplayName("retenirMeilleure : refusé s'il n'y a aucune proposition")
    void retenirMeilleure_refuse_sans_proposition() {
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(8000));
        when(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE)).thenReturn(List.of());

        assertThatThrownBy(() -> propositionService.retenirMeilleure("c1"))
                .isInstanceOf(IllegalStateException.class);
        verify(campagneService, never()).attribuer(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "CLOTUREE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("retenirMeilleure : refusé si la campagne n'est pas CLOTUREE")
    void retenirMeilleure_refuse_hors_cloturee(StatutCampagne statut) {
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", statut, LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> propositionService.retenirMeilleure("c1"))
                .isInstanceOf(IllegalStateException.class);
        verify(propositionRepository, never()).saveAll(any());
        verify(campagneService, never()).attribuer(any(), any());
    }

    @Test
    @DisplayName("retenirMeilleure : refusé si aucun volume n'a été commandé")
    void retenirMeilleure_refuse_sans_volume() {
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneCloturee(0));

        assertThatThrownBy(() -> propositionService.retenirMeilleure("c1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("volume");
        verify(propositionRepository, never()).saveAll(any());
        verify(campagneService, never()).attribuer(any(), any());
    }

    // ---------------------------------------------------------------- consulter

    @Test
    @DisplayName("trouverParId : proposition inexistante")
    void trouverParId_inexistante() {
        when(propositionRepository.findById("p1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propositionService.trouverParId("p1"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("listerParCampagne et listerParFournisseur : délèguent au repository")
    void listers_delegent_au_repository() {
        List<Proposition> deLaCampagne = List.of(unPrixUnique("p1", "1.05", LocalDateTime.now()));
        List<Proposition> duFournisseur = List.of(unPrixUnique("p2", "1.10", LocalDateTime.now()));
        when(propositionRepository.findByCampagneId("c1")).thenReturn(deLaCampagne);
        when(propositionRepository.findByFournisseurId("f1")).thenReturn(duFournisseur);

        assertThat(propositionService.listerParCampagne("c1")).isEqualTo(deLaCampagne);
        assertThat(propositionService.listerParFournisseur("f1")).isEqualTo(duFournisseur);
    }
}
