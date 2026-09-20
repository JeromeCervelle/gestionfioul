package fr.limoges.valadon.gestionfioul.participation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
class ParticipationServiceTest {

    @Mock
    private ParticipationRepository participationRepository;

    @Mock
    private CampagneService campagneService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ParticipationService participationService;

    private User unUtilisateur(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setAdresse("1 rue du Profil, 87000 Limoges");
        return user;
    }

    private Campagne uneCampagne(String id, StatutCampagne statut, LocalDateTime dateCloture) {
        Campagne campagne = new Campagne();
        campagne.setId(id);
        campagne.setStatut(statut);
        campagne.setDateCloture(dateCloture);
        return campagne;
    }

    private Campagne uneCampagneOuverte(String id) {
        return uneCampagne(id, StatutCampagne.OUVERTE, LocalDateTime.now().plusDays(7));
    }

    private Participation uneParticipation(String id, String campagneId, String clientId, StatutParticipation statut) {
        Participation participation = new Participation();
        participation.setId(id);
        participation.setCampagneId(campagneId);
        participation.setClientId(clientId);
        participation.setStatut(statut);
        return participation;
    }

    private void clientEtCampagneOuverte() {
        when(userService.trouverParId("u1")).thenReturn(unUtilisateur("u1", Role.CLIENT));
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneOuverte("c1"));
    }

    // -------------------------------------------------------------- participer

    @Test
    @DisplayName("participer : enregistre une participation confirmée")
    void participer_enregistre_une_participation_confirmee() {
        clientEtCampagneOuverte();
        when(participationRepository.save(any(Participation.class))).thenAnswer(i -> i.getArgument(0));

        Participation resultat = participationService.participer("c1", "u1", 6000, "2 rue de Livraison");

        assertThat(resultat.getCampagneId()).isEqualTo("c1");
        assertThat(resultat.getClientId()).isEqualTo("u1");
        assertThat(resultat.getVolumeSouhaite()).isEqualTo(6000);
        assertThat(resultat.getAdresseLivraison()).isEqualTo("2 rue de Livraison");
        assertThat(resultat.getStatut()).isEqualTo(StatutParticipation.CONFIRMEE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("participer : sans adresse de livraison, reprend celle du profil du client")
    void participer_reprend_l_adresse_du_profil(String adresse) {
        clientEtCampagneOuverte();
        when(participationRepository.save(any(Participation.class))).thenAnswer(i -> i.getArgument(0));

        Participation resultat = participationService.participer("c1", "u1", 1000, adresse);

        assertThat(resultat.getAdresseLivraison()).isEqualTo("1 rue du Profil, 87000 Limoges");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -500})
    @DisplayName("participer : un volume nul ou négatif est refusé avant tout accès aux données")
    void participer_volume_invalide_refuse(int volume) {
        assertThatThrownBy(() -> participationService.participer("c1", "u1", volume, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("volume");
        verifyNoInteractions(userService, campagneService, participationRepository);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "CLIENT", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("participer : seul un client peut participer")
    void participer_refuse_un_non_client(Role role) {
        when(userService.trouverParId("u1")).thenReturn(unUtilisateur("u1", role));

        assertThatThrownBy(() -> participationService.participer("c1", "u1", 1000, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(participationRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "OUVERTE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("participer : refusé si la campagne n'est pas OUVERTE")
    void participer_refuse_si_campagne_pas_ouverte(StatutCampagne statut) {
        when(userService.trouverParId("u1")).thenReturn(unUtilisateur("u1", Role.CLIENT));
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", statut, LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> participationService.participer("c1", "u1", 1000, null))
                .isInstanceOf(IllegalStateException.class);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("participer : refusé si la campagne est OUVERTE mais sa date limite est passée")
    void participer_refuse_si_date_limite_depassee() {
        when(userService.trouverParId("u1")).thenReturn(unUtilisateur("u1", Role.CLIENT));
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", StatutCampagne.OUVERTE, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> participationService.participer("c1", "u1", 1000, null))
                .isInstanceOf(IllegalStateException.class);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("participer : un client ne peut pas avoir deux participations confirmées dans la même campagne")
    void participer_refuse_un_doublon() {
        clientEtCampagneOuverte();
        when(participationRepository.existsByCampagneIdAndClientIdAndStatut("c1", "u1", StatutParticipation.CONFIRMEE))
                .thenReturn(true);

        assertThatThrownBy(() -> participationService.participer("c1", "u1", 1000, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà");
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("participer : client inexistant")
    void participer_client_inexistant() {
        when(userService.trouverParId("u1")).thenThrow(new NoSuchElementException("introuvable"));

        assertThatThrownBy(() -> participationService.participer("c1", "u1", 1000, null))
                .isInstanceOf(NoSuchElementException.class);
        verify(participationRepository, never()).save(any());
    }

    // ----------------------------------------------------------------- annuler

    @Test
    @DisplayName("annuler : passe la participation à ANNULEE")
    void annuler_passe_a_annulee() {
        when(participationRepository.findById("p1"))
                .thenReturn(Optional.of(uneParticipation("p1", "c1", "u1", StatutParticipation.CONFIRMEE)));
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagneOuverte("c1"));
        when(participationRepository.save(any(Participation.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(participationService.annuler("p1", "u1").getStatut()).isEqualTo(StatutParticipation.ANNULEE);
    }

    @Test
    @DisplayName("annuler : un client ne peut pas annuler la participation d'un autre")
    void annuler_refuse_pour_un_autre_client() {
        Participation participation = uneParticipation("p1", "c1", "u1", StatutParticipation.CONFIRMEE);
        when(participationRepository.findById("p1")).thenReturn(Optional.of(participation));

        assertThatThrownBy(() -> participationService.annuler("p1", "u2"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(participation.getStatut()).isEqualTo(StatutParticipation.CONFIRMEE);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("annuler : une participation déjà annulée ne peut pas l'être de nouveau")
    void annuler_refuse_si_deja_annulee() {
        when(participationRepository.findById("p1"))
                .thenReturn(Optional.of(uneParticipation("p1", "c1", "u1", StatutParticipation.ANNULEE)));

        assertThatThrownBy(() -> participationService.annuler("p1", "u1"))
                .isInstanceOf(IllegalStateException.class);
        verify(participationRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "OUVERTE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("annuler : impossible une fois la campagne clôturée")
    void annuler_refuse_si_campagne_pas_ouverte(StatutCampagne statut) {
        Participation participation = uneParticipation("p1", "c1", "u1", StatutParticipation.CONFIRMEE);
        when(participationRepository.findById("p1")).thenReturn(Optional.of(participation));
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", statut, LocalDateTime.now().plusDays(7)));

        assertThatThrownBy(() -> participationService.annuler("p1", "u1"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(participation.getStatut()).isEqualTo(StatutParticipation.CONFIRMEE);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("annuler : impossible après la date limite, même si la campagne est encore OUVERTE")
    void annuler_refuse_si_date_limite_depassee() {
        Participation participation = uneParticipation("p1", "c1", "u1", StatutParticipation.CONFIRMEE);
        when(participationRepository.findById("p1")).thenReturn(Optional.of(participation));
        when(campagneService.trouverParId("c1"))
                .thenReturn(uneCampagne("c1", StatutCampagne.OUVERTE, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> participationService.annuler("p1", "u1"))
                .isInstanceOf(IllegalStateException.class);
        verify(participationRepository, never()).save(any());
    }

    @Test
    @DisplayName("annuler : participation inexistante")
    void annuler_participation_inexistante() {
        when(participationRepository.findById("p1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.annuler("p1", "u1"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // -------------------------------------------------------------- consulter

    @Test
    @DisplayName("listerParCampagne et listerParClient : délèguent au repository")
    void listers_delegent_au_repository() {
        List<Participation> deLaCampagne = List.of(uneParticipation("p1", "c1", "u1", StatutParticipation.CONFIRMEE));
        List<Participation> duClient = List.of(uneParticipation("p2", "c2", "u1", StatutParticipation.ANNULEE));
        when(participationRepository.findByCampagneId("c1")).thenReturn(deLaCampagne);
        when(participationRepository.findByClientId("u1")).thenReturn(duClient);

        assertThat(participationService.listerParCampagne("c1")).isEqualTo(deLaCampagne);
        assertThat(participationService.listerParClient("u1")).isEqualTo(duClient);
    }
}
