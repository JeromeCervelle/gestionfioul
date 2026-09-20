package fr.limoges.valadon.gestionfioul.participation;

import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.ID_AUTRE_CLIENT;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.ID_CLIENT;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.autreClient;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.security.UtilisateursDeTest.fournisseur;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.security.SecurityConfig;

@WebMvcTest(ParticipationController.class)
@Import(SecurityConfig.class)
class ParticipationControllerTest {

    private static final String PARTICIPATION = """
            {"volumeSouhaite": 6000, "adresseLivraison": "2 rue B, 87000 Limoges"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipationService participationService;

    private Participation uneParticipation(String id, String campagneId, String clientId) {
        Participation participation = new Participation();
        participation.setId(id);
        participation.setCampagneId(campagneId);
        participation.setClientId(clientId);
        participation.setVolumeSouhaite(6000);
        participation.setStatut(StatutParticipation.CONFIRMEE);
        return participation;
    }

    // ---------------------------------------------------------------- participer

    @Test
    @DisplayName("POST /api/campagnes/{id}/participations : un client participe, 201")
    void participer_par_un_client() throws Exception {
        when(participationService.participer("c1", ID_CLIENT, 6000, "2 rue B, 87000 Limoges"))
                .thenReturn(uneParticipation("p1", "c1", ID_CLIENT));

        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content(PARTICIPATION).with(client()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.clientId").value(ID_CLIENT))
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"));
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/participations : le client est celui qui est connecté, jamais celui de la requête")
    void participer_ignore_le_client_de_la_requete() throws Exception {
        when(participationService.participer(anyString(), anyString(), anyInt(), any()))
                .thenReturn(uneParticipation("p1", "c1", ID_CLIENT));

        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).with(client()).content("""
                                {"clientId": "%s", "volumeSouhaite": 100}
                                """.formatted(ID_AUTRE_CLIENT)))
                .andExpect(status().isCreated());

        verify(participationService).participer("c1", ID_CLIENT, 100, null);
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/participations : réservé aux clients (admin et fournisseur 403, anonyme 401)")
    void participer_reserve_aux_clients() throws Exception {
        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content(PARTICIPATION).with(admin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content(PARTICIPATION).with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content(PARTICIPATION))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(participationService);
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/participations : un doublon ou une campagne fermée donne 409")
    void participer_refuse_par_le_service_donne_409() throws Exception {
        when(participationService.participer(anyString(), anyString(), anyInt(), any()))
                .thenThrow(new IllegalStateException("Ce client participe déjà à la campagne"));

        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content(PARTICIPATION).with(client()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ce client participe déjà à la campagne"));
    }

    @Test
    @DisplayName("POST /api/campagnes/{id}/participations : un volume invalide donne 400")
    void participer_volume_invalide_donne_400() throws Exception {
        when(participationService.participer(anyString(), anyString(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("Le volume doit être strictement positif"));

        mockMvc.perform(post("/api/campagnes/c1/participations")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"volumeSouhaite\": 0}").with(client()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------- lister par campagne

    @Test
    @DisplayName("GET /api/campagnes/{id}/participations : les commandes de tous les clients sont réservées à l'admin")
    void lister_par_campagne_reserve_a_l_admin() throws Exception {
        when(participationService.listerParCampagne("c1"))
                .thenReturn(List.of(uneParticipation("p1", "c1", ID_CLIENT), uneParticipation("p2", "c1", ID_AUTRE_CLIENT)));

        mockMvc.perform(get("/api/campagnes/c1/participations").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/campagnes/c1/participations").with(client()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/campagnes/c1/participations").with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/campagnes/c1/participations"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ mes participations

    @Test
    @DisplayName("GET /api/moi/participations : renvoie uniquement les participations du client connecté")
    void mes_participations() throws Exception {
        when(participationService.listerParClient(ID_CLIENT))
                .thenReturn(List.of(uneParticipation("p1", "c1", ID_CLIENT)));

        mockMvc.perform(get("/api/moi/participations").with(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientId").value(ID_CLIENT));

        verify(participationService).listerParClient(ID_CLIENT);
    }

    @Test
    @DisplayName("GET /api/moi/participations : un autre client obtient sa propre liste, pas celle du premier")
    void mes_participations_dependent_de_l_appelant() throws Exception {
        when(participationService.listerParClient(ID_AUTRE_CLIENT)).thenReturn(List.of());

        mockMvc.perform(get("/api/moi/participations").with(autreClient()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(participationService).listerParClient(ID_AUTRE_CLIENT);
    }

    @Test
    @DisplayName("GET /api/moi/participations : réservé aux clients (admin et fournisseur 403, anonyme 401)")
    void mes_participations_reservees_aux_clients() throws Exception {
        mockMvc.perform(get("/api/moi/participations").with(admin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/moi/participations").with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/moi/participations"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(participationService);
    }

    // ------------------------------------------------------------------- annuler

    @Test
    @DisplayName("POST /api/participations/{id}/annuler : le client annule sa participation, 200")
    void annuler_par_le_client() throws Exception {
        Participation annulee = uneParticipation("p1", "c1", ID_CLIENT);
        annulee.setStatut(StatutParticipation.ANNULEE);
        when(participationService.annuler("p1", ID_CLIENT)).thenReturn(annulee);

        mockMvc.perform(post("/api/participations/p1/annuler").with(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
    }

    @Test
    @DisplayName("POST /api/participations/{id}/annuler : l'appelant est transmis au service, qui refuse la participation d'un autre")
    void annuler_transmet_l_appelant_au_service() throws Exception {
        when(participationService.annuler("p1", ID_AUTRE_CLIENT))
                .thenThrow(new IllegalArgumentException("Cette participation n'appartient pas à ce client"));

        mockMvc.perform(post("/api/participations/p1/annuler").with(autreClient()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cette participation n'appartient pas à ce client"));

        verify(participationService).annuler("p1", ID_AUTRE_CLIENT);
    }

    @Test
    @DisplayName("POST /api/participations/{id}/annuler : une participation déjà annulée ou une campagne fermée donne 409")
    void annuler_impossible_donne_409() throws Exception {
        when(participationService.annuler("p1", ID_CLIENT))
                .thenThrow(new IllegalStateException("Cette participation est déjà annulée"));

        mockMvc.perform(post("/api/participations/p1/annuler").with(client()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/participations/{id}/annuler : réservé aux clients (admin et fournisseur 403, anonyme 401)")
    void annuler_reserve_aux_clients() throws Exception {
        mockMvc.perform(post("/api/participations/p1/annuler").with(admin()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/participations/p1/annuler").with(fournisseur()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/participations/p1/annuler"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(participationService);
    }
}
