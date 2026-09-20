package fr.limoges.valadon.gestionfioul.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import com.jayway.jsonpath.JsonPath;

import fr.limoges.valadon.gestionfioul.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.campagne.CampagneRepository;
import fr.limoges.valadon.gestionfioul.campagne.CampagneService;
import fr.limoges.valadon.gestionfioul.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.participation.Participation;
import fr.limoges.valadon.gestionfioul.participation.ParticipationRepository;
import fr.limoges.valadon.gestionfioul.participation.StatutParticipation;
import fr.limoges.valadon.gestionfioul.proposition.Palier;
import fr.limoges.valadon.gestionfioul.proposition.Proposition;
import fr.limoges.valadon.gestionfioul.proposition.PropositionRepository;
import fr.limoges.valadon.gestionfioul.proposition.StatutProposition;
import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;
import fr.limoges.valadon.gestionfioul.user.UserRepository;

/**
 * Test de bout en bout : application complète (filtres de sécurité, contrôleurs, services, repositories)
 * contre une vraie base MongoDB lancée par Testcontainers. Ignoré si Docker n'est pas disponible.
 */
@SpringBootTest(properties = {
        "gestionfioul.admin.email=admin@test.fr",
        "gestionfioul.admin.password=AdminTest2026"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ParcoursCommandeGroupeeTest {

    // @ServiceConnection branche l'application sur ce conteneur, à la place de l'URI d'application.properties
    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0.9");

    private static final String ADMIN_EMAIL = "admin@test.fr";
    private static final String ADMIN_MOT_DE_PASSE = "AdminTest2026";
    private static final String MOT_DE_PASSE = "motdepasse1";
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampagneRepository campagneRepository;

    @Autowired
    private ParticipationRepository participationRepository;

    @Autowired
    private PropositionRepository propositionRepository;

    @Autowired
    private CampagneService campagneService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Repart d'une base vide, en conservant l'admin créé au démarrage de l'application. */
    @BeforeEach
    void viderLaBase() {
        propositionRepository.deleteAll();
        participationRepository.deleteAll();
        campagneRepository.deleteAll();
        userRepository.deleteAll(userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .toList());
    }

    // ------------------------------------------------------------------ outils

    private RequestPostProcessor admin() {
        return httpBasic(ADMIN_EMAIL, ADMIN_MOT_DE_PASSE);
    }

    private RequestPostProcessor connecte(String email) {
        return httpBasic(email, MOT_DE_PASSE);
    }

    private String corps(MvcResult resultat) throws Exception {
        return resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String id(MvcResult resultat) throws Exception {
        return JsonPath.read(corps(resultat), "$.id");
    }

    private String inscrire(String email, String role, String raisonSociale) throws Exception {
        String champRaisonSociale = raisonSociale == null ? "" : ", \"raisonSociale\": \"" + raisonSociale + "\"";
        MvcResult resultat = mockMvc.perform(post("/api/users").contentType(JSON).content("""
                        {"email": "%s", "motDePasse": "%s", "nom": "Test", "role": "%s",
                         "adresse": "1 rue Test, 87000 Limoges"%s}
                        """.formatted(email, MOT_DE_PASSE, role, champRaisonSociale)))
                .andExpect(status().isCreated())
                .andReturn();
        return id(resultat);
    }

    private String creerCampagne() throws Exception {
        LocalDateTime maintenant = LocalDateTime.now();
        MvcResult resultat = mockMvc.perform(post("/api/campagnes").with(admin()).contentType(JSON).content("""
                        {"libelle": "Commande groupée d'hiver", "dateOuverture": "%s",
                         "dateCloture": "%s", "zoneLivraison": "87"}
                        """.formatted(maintenant.minusHours(1), maintenant.plusDays(2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("BROUILLON"))
                .andReturn();
        return id(resultat);
    }

    private String participer(String campagneId, String emailClient, int volume) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/campagnes/" + campagneId + "/participations")
                        .with(connecte(emailClient)).contentType(JSON)
                        .content("{\"volumeSouhaite\": " + volume + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"))
                .andReturn();
        return id(resultat);
    }

    private String soumettre(String campagneId, String emailFournisseur, String paliersJson) throws Exception {
        MvcResult resultat = mockMvc.perform(post("/api/campagnes/" + campagneId + "/propositions")
                        .with(connecte(emailFournisseur)).contentType(JSON).content("""
                                {"validiteJusqua": "%s", "paliers": %s}
                                """.formatted(LocalDateTime.now().plusDays(10), paliersJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SOUMISE"))
                .andReturn();
        return id(resultat);
    }

    // ------------------------------------------------------- parcours nominal

    @Test
    @DisplayName("parcours complet : inscription, campagne, participations, propositions, clôture, attribution, livraison")
    void parcours_complet_d_une_commande_groupee() throws Exception {
        inscrire("alice@test.fr", "CLIENT", null);
        inscrire("bob@test.fr", "CLIENT", null);
        inscrire("fioul1@test.fr", "FOURNISSEUR", "Fioul Limousin");
        inscrire("fioul2@test.fr", "FOURNISSEUR", "Énergie 87");

        // l'admin crée puis ouvre la campagne
        String campagneId = creerCampagne();
        mockMvc.perform(post("/api/campagnes/" + campagneId + "/ouvrir").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("OUVERTE"));
        mockMvc.perform(get("/api/campagnes").with(connecte("alice@test.fr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(campagneId));

        // les clients participent ; Alice ne peut pas participer deux fois
        participer(campagneId, "alice@test.fr", 3000);
        String participationBob = participer(campagneId, "bob@test.fr", 5000);
        mockMvc.perform(post("/api/campagnes/" + campagneId + "/participations")
                        .with(connecte("alice@test.fr")).contentType(JSON).content("{\"volumeSouhaite\": 100}"))
                .andExpect(status().isConflict());

        // Bob annule puis recommence avec un volume plus faible : seule la dernière participation compte
        mockMvc.perform(post("/api/participations/" + participationBob + "/annuler").with(connecte("bob@test.fr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
        participer(campagneId, "bob@test.fr", 4000);

        mockMvc.perform(get("/api/campagnes/" + campagneId + "/participations").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/api/moi/participations").with(connecte("alice@test.fr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // les fournisseurs proposent : le 1er a un barème dégressif, le 2e un prix unique
        String propositionFioul1 = soumettre(campagneId, "fioul1@test.fr", """
                [{"volumeMin": 0, "volumeMax": 5000, "prixLitre": 1.10},
                 {"volumeMin": 5000, "volumeMax": null, "prixLitre": 1.02}]
                """);
        soumettre(campagneId, "fioul2@test.fr", """
                [{"volumeMin": 0, "volumeMax": null, "prixLitre": 1.05}]
                """);
        mockMvc.perform(get("/api/campagnes/" + campagneId + "/propositions").with(connecte("fioul2@test.fr")))
                .andExpect(status().isForbidden());

        // clôture : le volume est figé à 3000 + 4000, la participation annulée de Bob est exclue
        mockMvc.perform(post("/api/campagnes/" + campagneId + "/cloturer").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTUREE"))
                .andExpect(jsonPath("$.volumeTotal").value(7000));
        mockMvc.perform(post("/api/campagnes/" + campagneId + "/participations")
                        .with(connecte("alice@test.fr")).contentType(JSON).content("{\"volumeSouhaite\": 100}"))
                .andExpect(status().isConflict());

        // à 7000 L, le barème du 1er fournisseur donne 1,02 €/L contre 1,05 €/L : il est retenu
        mockMvc.perform(post("/api/campagnes/" + campagneId + "/attribution").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(propositionFioul1))
                .andExpect(jsonPath("$.statut").value("RETENUE"))
                .andExpect(jsonPath("$.paliers[1].prixLitre").value(1.02));
        mockMvc.perform(get("/api/moi/propositions").with(connecte("fioul1@test.fr")))
                .andExpect(jsonPath("$[0].statut").value("RETENUE"));
        mockMvc.perform(get("/api/moi/propositions").with(connecte("fioul2@test.fr")))
                .andExpect(jsonPath("$[0].statut").value("REJETEE"));
        mockMvc.perform(get("/api/campagnes/" + campagneId).with(connecte("alice@test.fr")))
                .andExpect(jsonPath("$.statut").value("ATTRIBUEE"))
                .andExpect(jsonPath("$.propositionRetenueId").value(propositionFioul1));

        // livraison
        mockMvc.perform(post("/api/campagnes/" + campagneId + "/livrer").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("LIVREE"));

        // état final en base
        assertThat(participationRepository.findByCampagneIdAndStatut(campagneId, StatutParticipation.CONFIRMEE))
                .hasSize(2);
        assertThat(propositionRepository.findByCampagneId(campagneId))
                .extracting(Proposition::getStatut)
                .containsExactlyInAnyOrder(StatutProposition.RETENUE, StatutProposition.REJETEE);
    }

    // ------------------------------------------------ sécurité avec vrai HTTP Basic

    @Test
    @DisplayName("authentification réelle : mot de passe haché en base, identifiants vérifiés à chaque requête")
    void authentification_http_basic_reelle() throws Exception {
        String aliceId = inscrire("alice@test.fr", "CLIENT", null);
        String bobId = inscrire("bob@test.fr", "CLIENT", null);

        mockMvc.perform(get("/api/users/" + aliceId).with(connecte("alice@test.fr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.fr"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
        mockMvc.perform(get("/api/users/" + aliceId).with(httpBasic("alice@test.fr", "mauvais-mot-de-passe")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/" + aliceId).with(httpBasic("inconnu@test.fr", MOT_DE_PASSE)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/" + aliceId))
                .andExpect(status().isUnauthorized());
        // l'email est insensible à la casse à la connexion
        mockMvc.perform(get("/api/users/" + aliceId).with(httpBasic("ALICE@Test.FR", MOT_DE_PASSE)))
                .andExpect(status().isOk());
        // un utilisateur authentifié ne lit pas le profil d'un autre
        mockMvc.perform(get("/api/users/" + bobId).with(connecte("alice@test.fr")))
                .andExpect(status().isForbidden());

        User enBase = userRepository.findByEmail("alice@test.fr").orElseThrow();
        assertThat(enBase.getMotDePasse()).startsWith("$2").isNotEqualTo(MOT_DE_PASSE);
        assertThat(passwordEncoder.matches(MOT_DE_PASSE, enBase.getMotDePasse())).isTrue();
    }

    @Test
    @DisplayName("l'admin créé au démarrage peut se connecter et agir, sans pouvoir être demandé à l'inscription")
    void admin_cree_au_demarrage() throws Exception {
        assertThat(userRepository.findByEmail(ADMIN_EMAIL)).get()
                .extracting(User::getRole).isEqualTo(Role.ADMIN);

        mockMvc.perform(get("/api/campagnes").with(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/users").contentType(JSON).content("""
                        {"email": "faux-admin@test.fr", "motDePasse": "motdepasse1", "role": "ADMIN"}
                        """))
                .andExpect(status().isBadRequest());
        assertThat(userRepository.existsByEmail("faux-admin@test.fr")).isFalse();
    }

    @Test
    @DisplayName("inscription : email normalisé, doublon, mot de passe trop court refusés")
    void regles_d_inscription() throws Exception {
        inscrire("  Alice@Test.FR ", "CLIENT", null);
        assertThat(userRepository.existsByEmail("alice@test.fr")).isTrue();

        mockMvc.perform(post("/api/users").contentType(JSON).content("""
                        {"email": "alice@test.fr", "motDePasse": "motdepasse1", "role": "CLIENT"}
                        """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users").contentType(JSON).content("""
                        {"email": "bob@test.fr", "motDePasse": "court", "role": "CLIENT"}
                        """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/users").contentType(JSON).content("""
                        {"email": "fournisseur@test.fr", "motDePasse": "motdepasse1", "role": "FOURNISSEUR"}
                        """))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------- comportements propres à MongoDB

    @Test
    @DisplayName("MongoDB : l'index unique sur l'email empêche deux comptes identiques, même en contournant le service")
    void index_unique_sur_l_email() {
        userRepository.save(unUtilisateur("doublon@test.fr"));

        assertThatThrownBy(() -> userRepository.save(unUtilisateur("doublon@test.fr")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    @DisplayName("MongoDB : les prix (BigDecimal), les limites nulles et les dates survivent à un aller-retour en base")
    void proposition_aller_retour_en_base() {
        LocalDateTime validite = LocalDateTime.of(2026, 3, 1, 12, 30);
        Proposition proposition = new Proposition();
        proposition.setCampagneId("c1");
        proposition.setFournisseurId("f1");
        proposition.setValiditeJusqua(validite);
        proposition.setPaliers(List.of(
                new Palier(0, 5000, new BigDecimal("1.10")),
                new Palier(5000, null, new BigDecimal("1.02"))));
        String id = propositionRepository.save(proposition).getId();

        Proposition relue = propositionRepository.findById(id).orElseThrow();

        assertThat(relue.getValiditeJusqua()).isEqualTo(validite);
        assertThat(relue.getStatut()).isEqualTo(StatutProposition.SOUMISE);
        assertThat(relue.getPaliers()).hasSize(2);
        assertThat(relue.getPaliers().get(0).getPrixLitre()).isEqualByComparingTo("1.10");
        assertThat(relue.getPaliers().get(1).getPrixLitre()).isEqualByComparingTo("1.02");
        assertThat(relue.getPaliers().get(1).getVolumeMax()).isNull();
        assertThat(propositionRepository.findByCampagneIdAndStatut("c1", StatutProposition.SOUMISE)).hasSize(1);
        assertThat(propositionRepository.existsByCampagneIdAndFournisseurIdAndStatut("c1", "f1", StatutProposition.SOUMISE))
                .isTrue();
    }

    @Test
    @DisplayName("clôture automatique : seules les campagnes OUVERTE à date dépassée sont clôturées, volume calculé en base")
    void cloture_automatique_des_campagnes_echues() {
        LocalDateTime maintenant = LocalDateTime.now();
        Campagne echue = enregistrer(uneCampagne(StatutCampagne.OUVERTE, maintenant.minusHours(1)));
        Campagne enCours = enregistrer(uneCampagne(StatutCampagne.OUVERTE, maintenant.plusDays(2)));
        Campagne brouillonEchu = enregistrer(uneCampagne(StatutCampagne.BROUILLON, maintenant.minusHours(1)));

        participation(echue, "u1", 1000, StatutParticipation.CONFIRMEE);
        participation(echue, "u2", 2500, StatutParticipation.CONFIRMEE);
        participation(echue, "u3", 900, StatutParticipation.ANNULEE);
        participation(enCours, "u1", 777, StatutParticipation.CONFIRMEE);

        List<Campagne> cloturees = campagneService.cloturerCampagnesEchues();

        assertThat(cloturees).extracting(Campagne::getId).containsExactly(echue.getId());
        Campagne echueEnBase = campagneRepository.findById(echue.getId()).orElseThrow();
        assertThat(echueEnBase.getStatut()).isEqualTo(StatutCampagne.CLOTUREE);
        assertThat(echueEnBase.getVolumeTotal()).isEqualTo(3500);
        assertThat(campagneRepository.findById(enCours.getId()).orElseThrow().getStatut())
                .isEqualTo(StatutCampagne.OUVERTE);
        assertThat(campagneRepository.findById(brouillonEchu.getId()).orElseThrow().getStatut())
                .isEqualTo(StatutCampagne.BROUILLON);
    }

    // ------------------------------------------------------ données de test

    private User unUtilisateur(String email) {
        User user = new User();
        user.setEmail(email);
        user.setMotDePasse("hache");
        user.setRole(Role.CLIENT);
        return user;
    }

    private Campagne uneCampagne(StatutCampagne statut, LocalDateTime dateCloture) {
        Campagne campagne = new Campagne();
        campagne.setLibelle("Campagne de test");
        campagne.setDateOuverture(LocalDateTime.now().minusDays(3));
        campagne.setDateCloture(dateCloture);
        campagne.setStatut(statut);
        return campagne;
    }

    private Campagne enregistrer(Campagne campagne) {
        return campagneRepository.save(campagne);
    }

    private void participation(Campagne campagne, String clientId, int volume, StatutParticipation statut) {
        Participation participation = new Participation();
        participation.setCampagneId(campagne.getId());
        participation.setClientId(clientId);
        participation.setVolumeSouhaite(volume);
        participation.setStatut(statut);
        participationRepository.save(participation);
    }
}
