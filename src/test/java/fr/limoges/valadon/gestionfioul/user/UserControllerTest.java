package fr.limoges.valadon.gestionfioul.user;

import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.ID_CLIENT;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.ID_FOURNISSEUR;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.autreClient;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.fournisseur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.security.SecurityConfig;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String INSCRIPTION_CLIENT = """
            {
              "email": "client@test.fr",
              "motDePasse": "motdepasse1",
              "nom": "Durand",
              "prenom": "Alice",
              "telephone": "0600000000",
              "role": "CLIENT",
              "adresse": "1 rue A, 87000 Limoges"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private User unUtilisateur(String id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.fr");
        user.setMotDePasse("HASH-A-NE-PAS-EXPOSER");
        user.setNom("Durand");
        user.setRole(role);
        return user;
    }

    // ------------------------------------------------------------ inscription

    @Test
    @DisplayName("POST /api/users : route publique, renvoie 201 sans jamais exposer le mot de passe")
    void inscription_publique_ne_renvoie_pas_le_mot_de_passe() throws Exception {
        when(userService.inscrire(any(User.class))).thenReturn(unUtilisateur("u1", Role.CLIENT));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(INSCRIPTION_CLIENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("u1"))
                .andExpect(jsonPath("$.email").value("u1@test.fr"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/users : les champs de la requête sont transmis au service")
    void inscription_transmet_les_champs_au_service() throws Exception {
        when(userService.inscrire(any(User.class))).thenReturn(unUtilisateur("u1", Role.CLIENT));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(INSCRIPTION_CLIENT))
                .andExpect(status().isCreated());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).inscrire(captor.capture());
        User transmis = captor.getValue();
        assertThat(transmis.getEmail()).isEqualTo("client@test.fr");
        assertThat(transmis.getMotDePasse()).isEqualTo("motdepasse1");
        assertThat(transmis.getRole()).isEqualTo(Role.CLIENT);
        assertThat(transmis.getAdresse()).isEqualTo("1 rue A, 87000 Limoges");
    }

    @Test
    @DisplayName("POST /api/users : un id fourni dans la requête est ignoré")
    void inscription_ignore_un_id_fourni() throws Exception {
        when(userService.inscrire(any(User.class))).thenReturn(unUtilisateur("u1", Role.CLIENT));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"id": "u-existant", "email": "a@test.fr", "motDePasse": "motdepasse1", "role": "CLIENT"}
                        """))
                .andExpect(status().isCreated());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).inscrire(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    @DisplayName("POST /api/users : le rôle ADMIN ne peut pas être demandé à l'inscription")
    void inscription_refuse_le_role_admin() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "a@test.fr", "motDePasse": "motdepasse1", "role": "ADMIN"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Le rôle ADMIN ne peut pas être demandé à l'inscription"));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("POST /api/users : une donnée refusée par le service donne 400 avec son message")
    void inscription_donnee_invalide_donne_400() throws Exception {
        when(userService.inscrire(any(User.class)))
                .thenThrow(new IllegalArgumentException("Un compte existe déjà avec cet email"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(INSCRIPTION_CLIENT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Un compte existe déjà avec cet email"));
    }

    @Test
    @DisplayName("POST /api/users : un rôle inconnu donne 400")
    void inscription_role_inconnu_donne_400() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "a@test.fr", "motDePasse": "motdepasse1", "role": "PIRATE"}
                        """))
                .andExpect(status().isBadRequest());

        verify(userService, never()).inscrire(any());
    }

    // ---------------------------------------------------------------- profil

    @Test
    @DisplayName("GET /api/users/{id} : sans authentification, 401")
    void profil_sans_authentification() throws Exception {
        mockMvc.perform(get("/api/users/" + ID_CLIENT))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /api/users/{id} : un utilisateur voit son propre profil, sans mot de passe")
    void profil_de_soi_meme() throws Exception {
        when(userService.trouverParId(ID_CLIENT)).thenReturn(unUtilisateur(ID_CLIENT, Role.CLIENT));

        mockMvc.perform(get("/api/users/" + ID_CLIENT).with(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID_CLIENT))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id} : le profil d'un autre utilisateur est interdit, à un client comme à un fournisseur")
    void profil_d_un_autre_est_interdit() throws Exception {
        mockMvc.perform(get("/api/users/" + ID_CLIENT).with(autreClient()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/" + ID_CLIENT).with(fournisseur()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /api/users/{id} : l'admin peut voir n'importe quel profil")
    void profil_visible_par_l_admin() throws Exception {
        when(userService.trouverParId(ID_FOURNISSEUR)).thenReturn(unUtilisateur(ID_FOURNISSEUR, Role.FOURNISSEUR));

        mockMvc.perform(get("/api/users/" + ID_FOURNISSEUR).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("FOURNISSEUR"));
    }

    @Test
    @DisplayName("GET /api/users/{id} : utilisateur inexistant, 404")
    void profil_inexistant() throws Exception {
        when(userService.trouverParId("inconnu")).thenThrow(new NoSuchElementException("Utilisateur introuvable : inconnu"));

        mockMvc.perform(get("/api/users/inconnu").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Utilisateur introuvable : inconnu"));
    }
}
