package fr.limoges.valadon.gestionfioul.proposition;

import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.ID_FOURNISSEUR;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.fournisseur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

@WebMvcTest(PropositionController.class)
@Import(SecurityConfig.class)
class PropositionControllerTest {

    private static final String SOUMISSION = """
            {
              "validiteJusqua": "2026-02-15T18:00:00",
              "paliers": [
                {"volumeMin": 0, "volumeMax": 5000, "prixLitre": 1.10},
                {"volumeMin": 5000, "volumeMax": null, "prixLitre": 1.05}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropositionService propositionService;

    private Proposition uneProposition(String id, String fournisseurId, StatutProposition statut) {
        Proposition proposition = new Proposition();
        proposition.setId(id);
        proposition.setCampagneId("c1");
        proposition.setFournisseurId(fournisseurId);
        proposition.setStatut(statut);
        proposition.setPaliers(List.of(
                new Palier(0, 5000, new BigDecimal("1.10")),
                new Palier(5000, null, new BigDecimal("1.05"))));
        return proposition;
    }

    // ----------------------------------------------------------------- soumettre

    @Test
    @DisplayName("POST /api/campagnes/{id}/propositions : un fournisseur soumet un barème, 201")
    void soumettre_par_un_fournisseur() throws Exception {
        when(propositionService.soumettre(any(), any(), any(), any()))
                .thenReturn(uneProposition("p1", ID_FOURNISSEUR, StatutProposition.SOUMISE));

        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION).with(fournisseur()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.statut").value("SOUMISE"))
                .andExpect(jsonPath("$.paliers.length()").value(2))
                .andExpect(jsonPath("$.paliers[1].volumeMax").value((Object) null));
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/propositions : la campagne, le fournisseur connecté, la validité et les paliers sont transmis")
    @SuppressWarnings("unchecked")
    void soumettre_transmet_les_bons_arguments() throws Exception {
        when(propositionService.soumettre(any(), any(), any(), any()))
                .thenReturn(uneProposition("p1", ID_FOURNISSEUR, StatutProposition.SOUMISE));

        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION).with(fournisseur()))
                .andExpect(status().isCreated());

        ArgumentCaptor<List<Palier>> paliers = ArgumentCaptor.forClass(List.class);
        verify(propositionService).soumettre(
                eq("c1"),
                eq(ID_FOURNISSEUR),
                eq(LocalDateTime.of(2026, 2, 15, 18, 0)),
                paliers.capture());
        assertThat(paliers.getValue()).hasSize(2);
        assertThat(paliers.getValue().get(0).getVolumeMin()).isZero();
        assertThat(paliers.getValue().get(0).getVolumeMax()).isEqualTo(5000);
        assertThat(paliers.getValue().get(0).getPrixLitre()).isEqualByComparingTo("1.10");
        assertThat(paliers.getValue().get(1).getVolumeMax()).isNull();
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/propositions : le fournisseur est celui qui est connecté, jamais celui de la requête")
    void soumettre_ignore_le_fournisseur_de_la_requete() throws Exception {
        when(propositionService.soumettre(any(), any(), any(), any()))
                .thenReturn(uneProposition("p1", ID_FOURNISSEUR, StatutProposition.SOUMISE));

        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).with(fournisseur()).content("""
                                {"fournisseurId": "un-concurrent", "validiteJusqua": "2026-02-15T18:00:00",
                                 "paliers": [{"volumeMin": 0, "prixLitre": 1.00}]}
                                """))
                .andExpect(status().isCreated());

        verify(propositionService).soumettre(
                eq("c1"),
                eq(ID_FOURNISSEUR),
                any(), any());
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/propositions : réservé aux fournisseurs (admin et client 403, anonyme 401)")
    void soumettre_reserve_aux_fournisseurs() throws Exception {
        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION).with(admin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION).with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(propositionService);
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/propositions : un barème refusé donne 400, une campagne fermée ou un doublon 409")
    void soumettre_refus_du_service() throws Exception {
        when(propositionService.soumettre(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Les paliers doivent se suivre sans trou ni chevauchement"))
                .thenThrow(new IllegalStateException("Ce fournisseur a déjà soumis une proposition pour cette campagne"));

        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION).with(fournisseur()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Les paliers doivent se suivre sans trou ni chevauchement"));
        mockMvc.perform(post("/api/campagnes/c1/propositions")
                        .contentType(MediaType.APPLICATION_JSON).content(SOUMISSION).with(fournisseur()))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------- lister par campagne

    @Test
    @DisplayName("GET /api/campagnes/{id}/propositions : les barèmes des concurrents sont réservés à l'admin")
    void lister_par_campagne_reserve_a_l_admin() throws Exception {
        when(propositionService.listerParCampagne("c1")).thenReturn(List.of(
                uneProposition("p1", ID_FOURNISSEUR, StatutProposition.SOUMISE),
                uneProposition("p2", "fournisseur-2", StatutProposition.SOUMISE)));

        mockMvc.perform(get("/api/campagnes/c1/propositions").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/campagnes/c1/propositions").with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/campagnes/c1/propositions").with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/campagnes/c1/propositions"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------- mes propositions

    @Test
    @DisplayName("GET /api/moi/propositions : renvoie uniquement les propositions du fournisseur connecté")
    void mes_propositions() throws Exception {
        when(propositionService.listerParFournisseur(ID_FOURNISSEUR))
                .thenReturn(List.of(uneProposition("p1", ID_FOURNISSEUR, StatutProposition.RETENUE)));

        mockMvc.perform(get("/api/moi/propositions").with(fournisseur()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fournisseurId").value(ID_FOURNISSEUR))
                .andExpect(jsonPath("$[0].statut").value("RETENUE"));

        verify(propositionService).listerParFournisseur(ID_FOURNISSEUR);
    }

    @Test
    @DisplayName("GET /api/moi/propositions : réservé aux fournisseurs (admin et client 403, anonyme 401)")
    void mes_propositions_reservees_aux_fournisseurs() throws Exception {
        mockMvc.perform(get("/api/moi/propositions").with(admin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/moi/propositions").with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/moi/propositions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(propositionService);
    }

    // ----------------------------------------------------------- attribution

    @Test
    @DisplayName("POST /api/campagnes/{id}/attribution : l'admin obtient la proposition retenue")
    void attribution_par_l_admin() throws Exception {
        when(propositionService.retenirMeilleure("c1"))
                .thenReturn(uneProposition("p1", ID_FOURNISSEUR, StatutProposition.RETENUE));

        mockMvc.perform(post("/api/campagnes/c1/attribution").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.statut").value("RETENUE"));
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/attribution : réservé à l'admin (client et fournisseur 403, anonyme 401)")
    void attribution_reservee_a_l_admin() throws Exception {
        mockMvc.perform(post("/api/campagnes/c1/attribution").with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/attribution").with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/attribution"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(propositionService);
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/attribution : campagne pas clôturée ou aucune proposition valable, 409")
    void attribution_impossible_donne_409() throws Exception {
        when(propositionService.retenirMeilleure("c1"))
                .thenThrow(new IllegalStateException("La campagne doit être clôturée avant d'être attribuée"));

        mockMvc.perform(post("/api/campagnes/c1/attribution").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La campagne doit être clôturée avant d'être attribuée"));
    }
}
