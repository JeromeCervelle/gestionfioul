package fr.limoges.valadon.gestionfioul.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User unClient() {
        User user = new User();
        user.setEmail("  Client@Test.FR ");
        user.setMotDePasse("motdepasse1");
        user.setNom("Durand");
        user.setRole(Role.CLIENT);
        return user;
    }

    private User unFournisseur(String raisonSociale) {
        User user = unClient();
        user.setRole(Role.FOURNISSEUR);
        user.setRaisonSociale(raisonSociale);
        return user;
    }

    @Test
    @DisplayName("inscrire : normalise l'email, hache le mot de passe et enregistre")
    void inscrire_client_normalise_email_hache_mot_de_passe_et_enregistre() {
        User user = unClient();
        when(passwordEncoder.encode("motdepasse1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User resultat = userService.inscrire(user);

        assertThat(resultat.getEmail()).isEqualTo("client@test.fr");
        assertThat(resultat.getMotDePasse()).isEqualTo("HASH");
        verify(userRepository).existsByEmail("client@test.fr");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("inscrire : un client n'a jamais de raison sociale")
    void inscrire_client_efface_la_raison_sociale() {
        User user = unClient();
        user.setRaisonSociale("Ne doit pas rester");
        when(passwordEncoder.encode("motdepasse1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(userService.inscrire(user).getRaisonSociale()).isNull();
    }

    @Test
    @DisplayName("inscrire : un fournisseur conserve sa raison sociale")
    void inscrire_fournisseur_conserve_la_raison_sociale() {
        User user = unFournisseur("Fioul SA");
        when(passwordEncoder.encode("motdepasse1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(userService.inscrire(user).getRaisonSociale()).isEqualTo("Fioul SA");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("inscrire : un fournisseur sans raison sociale est refusé")
    void inscrire_fournisseur_sans_raison_sociale_refuse(String raisonSociale) {
        assertThatThrownBy(() -> userService.inscrire(unFournisseur(raisonSociale)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raison sociale");
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("inscrire : un email absent ou vide est refusé")
    void inscrire_email_absent_refuse(String email) {
        User user = unClient();
        user.setEmail(email);

        assertThatThrownBy(() -> userService.inscrire(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "1234567"})
    @DisplayName("inscrire : un mot de passe de moins de 8 caractères est refusé")
    void inscrire_mot_de_passe_trop_court_refuse(String motDePasse) {
        User user = unClient();
        user.setMotDePasse(motDePasse);

        assertThatThrownBy(() -> userService.inscrire(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mot de passe");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("inscrire : un mot de passe de exactement 8 caractères est accepté")
    void inscrire_mot_de_passe_de_8_caracteres_accepte() {
        User user = unClient();
        user.setMotDePasse("12345678");
        when(passwordEncoder.encode("12345678")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(userService.inscrire(user).getMotDePasse()).isEqualTo("HASH");
    }

    @Test
    @DisplayName("inscrire : le rôle est obligatoire")
    void inscrire_role_absent_refuse() {
        User user = unClient();
        user.setRole(null);

        assertThatThrownBy(() -> userService.inscrire(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rôle");
        verify(userRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("inscrire : un email déjà utilisé est refusé, quel que soit le rôle")
    void inscrire_email_deja_pris_refuse(Role role) {
        User user = unFournisseur("Fioul SA");
        user.setRole(role);
        when(userRepository.existsByEmail("client@test.fr")).thenReturn(true);

        assertThatThrownBy(() -> userService.inscrire(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existe déjà");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("trouverParId : renvoie l'utilisateur")
    void trouverParId_renvoie_l_utilisateur() {
        User user = unClient();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        assertThat(userService.trouverParId("u1")).isSameAs(user);
    }

    @Test
    @DisplayName("trouverParId : utilisateur inexistant")
    void trouverParId_inexistant() {
        when(userRepository.findById("u1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.trouverParId("u1"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("trouverParEmail : normalise l'email avant de chercher")
    void trouverParEmail_normalise_l_email() {
        User user = unClient();
        when(userRepository.findByEmail("client@test.fr")).thenReturn(Optional.of(user));

        assertThat(userService.trouverParEmail("  Client@Test.FR ")).isSameAs(user);
    }

    @Test
    @DisplayName("trouverParEmail : utilisateur inexistant")
    void trouverParEmail_inexistant() {
        when(userRepository.findByEmail("inconnu@test.fr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.trouverParEmail("inconnu@test.fr"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
