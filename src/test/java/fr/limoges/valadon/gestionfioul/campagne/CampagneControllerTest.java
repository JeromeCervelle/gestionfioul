package fr.limoges.valadon.gestionfioul.campagne;

import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.fournisseur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.security.SecurityConfig;

@WebMvcTest(CampagneController.class)
@Import(SecurityConfig.class)
class CampagneControllerTest {

    private static final String CREATION = """
            {
              "libelle": "Commande groupée janvier",
              "dateOuverture": "2026-01-05T08:00:00",
              "dateCloture": "2026-01-31T23:59:00",
              "zoneLivraison": "87"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampagneService campagneService;

    private Campagne uneCampagne(String id, StatutCampagne statut) {
        Campagne campagne = new Campagne();
        campagne.setId(id);
        campagne.setLibelle("Janvier");
        campagne.setStatut(statut);
        return campagne;
    }

    @BeforeEach
    void serviceRepondPourChaqueAction() {
        when(campagneService.ouvrir("c1")).thenReturn(uneCampagne("c1", StatutCampagne.OUVERTE));
        when(campagneService.cloturer("c1")).thenReturn(uneCampagne("c1", StatutCampagne.CLOTUREE));
        when(campagneService.marquerLivree("c1")).thenReturn(uneCampagne("c1", StatutCampagne.LIVREE));
        when(campagneService.annuler("c1")).thenReturn(uneCampagne("c1", StatutCampagne.ANNULEE));
    }

    // ------------------------------------------------------------ consultation

    @Test
    @DisplayName("GET /api/campagnes : sans authentification, 401")
    void liste_sans_authentification() throws Exception {
        mockMvc.perform(get("/api/campagnes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campagneService);
    }

    @Test
    @DisplayName("GET /api/campagnes : sans paramètre, liste les campagnes OUVERTE, pour tout rôle")
    void liste_par_defaut_les_campagnes_ouvertes() throws Exception {
        when(campagneService.listerParStatut(StatutCampagne.OUVERTE))
                .thenReturn(List.of(uneCampagne("c1", StatutCampagne.OUVERTE)));

        mockMvc.perform(get("/api/campagnes").with(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("c1"));
        mockMvc.perform(get("/api/campagnes").with(fournisseur()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/campagnes?statut= : filtre par statut")
    void liste_filtre_par_statut() throws Exception {
        when(campagneService.listerParStatut(StatutCampagne.CLOTUREE))
                .thenReturn(List.of(uneCampagne("c2", StatutCampagne.CLOTUREE)));

        mockMvc.perform(get("/api/campagnes").param("statut", "CLOTUREE").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("CLOTUREE"));
    }

    @Test
    @DisplayName("GET /api/campagnes?statut= : un statut inconnu donne 400")
    void liste_statut_inconnu() throws Exception {
        mockMvc.perform(get("/api/campagnes").param("statut", "NIMPORTEQUOI").with(admin()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(campagneService);
    }

    @Test
    @DisplayName("GET /api/campagnes/{id} : consultation ouverte à tout utilisateur connecté")
    void consultation_d_une_campagne() throws Exception {
        when(campagneService.trouverParId("c1")).thenReturn(uneCampagne("c1", StatutCampagne.OUVERTE));

        mockMvc.perform(get("/api/campagnes/c1").with(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"));
    }

    @Test
    @DisplayName("GET /api/campagnes/{id} : campagne inexistante, 404")
    void consultation_campagne_inexistante() throws Exception {
        when(campagneService.trouverParId("nope")).thenThrow(new NoSuchElementException("Campagne introuvable : nope"));

        mockMvc.perform(get("/api/campagnes/nope").with(client()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Campagne introuvable : nope"));
    }

    // ---------------------------------------------------------------- création

    @Test
    @DisplayName("POST /api/campagnes : l'admin crée une campagne, 201")
    void creation_par_l_admin() throws Exception {
        when(campagneService.creer(any(Campagne.class))).thenReturn(uneCampagne("c1", StatutCampagne.BROUILLON));

        mockMvc.perform(post("/api/campagnes").contentType(MediaType.APPLICATION_JSON).content(CREATION).with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.statut").value("BROUILLON"));

        ArgumentCaptor<Campagne> captor = ArgumentCaptor.forClass(Campagne.class);
        verify(campagneService).creer(captor.capture());
        Campagne transmise = captor.getValue();
        assertThat(transmise.getLibelle()).isEqualTo("Commande groupée janvier");
        assertThat(transmise.getDateOuverture()).isEqualTo(LocalDateTime.of(2026, 1, 5, 8, 0));
        assertThat(transmise.getDateCloture()).isEqualTo(LocalDateTime.of(2026, 1, 31, 23, 59));
        assertThat(transmise.getZoneLivraison()).isEqualTo("87");
    }

    @Test
    @DisplayName("POST /api/campagnes : id, statut, volume et proposition retenue fournis sont ignorés")
    void creation_ignore_les_champs_reserves() throws Exception {
        when(campagneService.creer(any(Campagne.class))).thenReturn(uneCampagne("c1", StatutCampagne.BROUILLON));

        mockMvc.perform(post("/api/campagnes").contentType(MediaType.APPLICATION_JSON).with(admin()).content("""
                        {
                          "id": "campagne-existante",
                          "libelle": "Pirate",
                          "dateOuverture": "2026-01-05T08:00:00",
                          "dateCloture": "2026-01-31T23:59:00",
                          "statut": "ATTRIBUEE",
                          "volumeTotal": 999999,
                          "propositionRetenueId": "p-truquee"
                        }
                        """))
                .andExpect(status().isCreated());

        ArgumentCaptor<Campagne> captor = ArgumentCaptor.forClass(Campagne.class);
        verify(campagneService).creer(captor.capture());
        Campagne transmise = captor.getValue();
        assertThat(transmise.getId()).isNull();
        assertThat(transmise.getStatut()).isEqualTo(StatutCampagne.BROUILLON);
        assertThat(transmise.getVolumeTotal()).isZero();
        assertThat(transmise.getPropositionRetenueId()).isNull();
    }

    @Test
    @DisplayName("POST /api/campagnes : réservé à l'admin (client et fournisseur 403, anonyme 401)")
    void creation_reservee_a_l_admin() throws Exception {
        mockMvc.perform(post("/api/campagnes").contentType(MediaType.APPLICATION_JSON).content(CREATION).with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes").contentType(MediaType.APPLICATION_JSON).content(CREATION).with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes").contentType(MediaType.APPLICATION_JSON).content(CREATION))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campagneService);
    }

    @Test
    @DisplayName("POST /api/campagnes : une donnée refusée par le service donne 400")
    void creation_donnee_invalide() throws Exception {
        when(campagneService.creer(any(Campagne.class)))
                .thenThrow(new IllegalArgumentException("La clôture doit être postérieure à l'ouverture"));

        mockMvc.perform(post("/api/campagnes").contentType(MediaType.APPLICATION_JSON).content(CREATION).with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La clôture doit être postérieure à l'ouverture"));
    }

    // ---------------------------------------------------- changements d'état

    @ParameterizedTest
    @ValueSource(strings = {"ouvrir", "cloturer", "livrer", "annuler"})
    @DisplayName("POST /api/campagnes/{id}/{action} : l'admin peut changer l'état")
    void action_par_l_admin(String action) throws Exception {
        mockMvc.perform(post("/api/campagnes/c1/" + action).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("c1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ouvrir", "cloturer", "livrer", "annuler"})
    @DisplayName("POST /api/campagnes/{id}/{action} : réservé à l'admin (client et fournisseur 403, anonyme 401)")
    void action_reservee_a_l_admin(String action) throws Exception {
        mockMvc.perform(post("/api/campagnes/c1/" + action).with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/" + action).with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/" + action))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campagneService);
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/cloturer : une transition impossible donne 409")
    void action_transition_impossible() throws Exception {
        when(campagneService.cloturer("c1"))
                .thenThrow(new IllegalStateException("La campagne est BROUILLON, elle devrait être OUVERTE"));

        mockMvc.perform(post("/api/campagnes/c1/cloturer").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La campagne est BROUILLON, elle devrait être OUVERTE"));
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/ouvrir : campagne inexistante, 404")
    void action_campagne_inexistante() throws Exception {
        when(campagneService.ouvrir("nope")).thenThrow(new NoSuchElementException("Campagne introuvable : nope"));

        mockMvc.perform(post("/api/campagnes/nope/ouvrir").with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("aucune route n'expose l'attribution directe d'une proposition par la campagne")
    void pas_de_route_d_attribution_directe() throws Exception {
        mockMvc.perform(post("/api/campagnes/c1/attribuer").param("propositionRetenueId", "p1").with(admin()))
                .andExpect(status().isNotFound());
    }
}
