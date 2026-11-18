package fr.limoges.valadon.gestionfioul.proposition;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import fr.limoges.valadon.gestionfioul.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.campagne.CampagneService;
import fr.limoges.valadon.gestionfioul.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.user.Role;
import fr.limoges.valadon.gestionfioul.user.User;
import fr.limoges.valadon.gestionfioul.user.UserService;

/**
 * Règles métier des propositions : dépôt d'un barème cohérent et choix de la moins-disante.
 *
 * <p>Les exceptions sont traduites en HTTP par {@link fr.limoges.valadon.gestionfioul.common.GestionExceptionHandler GestionExceptionHandler}.</p>
 */
@Service
public class PropositionService {

    private final PropositionRepository propositionRepository;
    private final CampagneService campagneService;
    private final UserService userService;

    /**
     * Crée le service.
     *
     * @param propositionRepository accès aux propositions
     * @param campagneService       pour vérifier le statut de la campagne et l'attribuer
     * @param userService           pour vérifier le rôle du fournisseur
     */
    public PropositionService(PropositionRepository propositionRepository,
                              CampagneService campagneService,
                              UserService userService) {
        this.propositionRepository = propositionRepository;
        this.campagneService = campagneService;
        this.userService = userService;
    }

    /**
     * Enregistre le barème d'un fournisseur, paliers triés par volume.
     *
     * @param campagneId     identifiant de la campagne
     * @param fournisseurId  identifiant du fournisseur connecté
     * @param validiteJusqua date jusqu'à laquelle les prix sont garantis, postérieure à la clôture
     * @param paliers        paliers du barème : de 0 L, sans trou ni chevauchement, le dernier sans limite
     * @return la proposition enregistrée, au statut {@code SOUMISE}
     * @throws IllegalArgumentException utilisateur qui n'est pas un fournisseur, validité trop courte,
     *                                  paliers incohérents ou prix non positif
     * @throws NoSuchElementException   fournisseur ou campagne inexistants
     * @throws IllegalStateException    campagne fermée, ou proposition déjà soumise par ce fournisseur
     */
    public Proposition soumettre(String campagneId, String fournisseurId,
                                 LocalDateTime validiteJusqua, List<Palier> paliers) {
        User fournisseur = userService.trouverParId(fournisseurId);
        if (fournisseur.getRole() != Role.FOURNISSEUR) {
            throw new IllegalArgumentException("Seul un fournisseur peut soumettre une proposition");
        }

        Campagne campagne = campagneService.trouverParId(campagneId);
        if (campagne.getStatut() != StatutCampagne.OUVERTE
                || campagne.getDateCloture().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("La campagne n'accepte plus de propositions");
        }

        // la proposition doit encore être valable quand la campagne sera attribuée, après la clôture
        if (validiteJusqua == null || !validiteJusqua.isAfter(campagne.getDateCloture())) {
            throw new IllegalArgumentException("La proposition doit être valable au-delà de la clôture de la campagne");
        }

        if (propositionRepository.existsByCampagneIdAndFournisseurIdAndStatut(
                campagneId, fournisseurId, StatutProposition.SOUMISE)) {
            throw new IllegalStateException("Ce fournisseur a déjà soumis une proposition pour cette campagne");
        }

        Proposition proposition = new Proposition();
        proposition.setCampagneId(campagneId);
        proposition.setFournisseurId(fournisseurId);
        proposition.setValiditeJusqua(validiteJusqua);
        proposition.setPaliers(verifierEtTrierPaliers(paliers));
        return propositionRepository.save(proposition);
    }

    /**
     * Choisit la moins-disante parmi les propositions soumises et encore valables, au {@code volumeTotal}
     * figé de la campagne. À prix égal, la première soumise l'emporte.
     *
     * <p>La retenue passe à {@code RETENUE}, toutes les autres propositions soumises à {@code REJETEE},
     * et la campagne passe à {@code ATTRIBUEE}.</p>
     *
     * @param campagneId identifiant de la campagne
     * @return la proposition retenue
     * @throws NoSuchElementException campagne inexistante
     * @throws IllegalStateException  campagne pas clôturée, volume total nul, ou aucune proposition valable
     */
    public Proposition retenirMeilleure(String campagneId) {
        Campagne campagne = campagneService.trouverParId(campagneId);
        if (campagne.getStatut() != StatutCampagne.CLOTUREE) {
            throw new IllegalStateException("La campagne doit être clôturée avant d'être attribuée");
        }
        int volumeTotal = campagne.getVolumeTotal();
        if (volumeTotal <= 0) {
            throw new IllegalStateException("Aucun volume commandé : rien à attribuer");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        List<Proposition> soumises =
                propositionRepository.findByCampagneIdAndStatut(campagneId, StatutProposition.SOUMISE);

        Proposition gagnante = soumises.stream()
                .filter(p -> !p.getValiditeJusqua().isBefore(maintenant))
                .filter(p -> prixPour(p, volumeTotal).isPresent())
                .min(Comparator.comparing((Proposition p) -> prixPour(p, volumeTotal).get())
                        .thenComparing(Proposition::getDateProposition))
                .orElseThrow(() -> new IllegalStateException("Aucune proposition valable pour cette campagne"));

        soumises.forEach(p -> p.setStatut(p == gagnante ? StatutProposition.RETENUE : StatutProposition.REJETEE));
        propositionRepository.saveAll(soumises);
        campagneService.attribuer(campagneId, gagnante.getId());
        return gagnante;
    }

    /**
     * Prix au litre du palier qui contient ce volume, appliqué à tout le volume
     * (le tarif ne s'applique pas tranche par tranche).
     *
     * @param proposition barème à appliquer
     * @param volume      volume en litres
     * @return le prix au litre, ou un {@code Optional} vide si aucun palier ne contient ce volume
     */
    public Optional<BigDecimal> prixPour(Proposition proposition, int volume) {
        return proposition.getPaliers().stream()
                .filter(p -> volume >= p.getVolumeMin()
                        && (p.getVolumeMax() == null || volume < p.getVolumeMax()))
                .map(Palier::getPrixLitre)
                .findFirst();
    }

    /**
     * Cherche une proposition par son id.
     *
     * <p>N'est appelée par aucune route : seul un test l'utilise (code mort repéré par JaCoCo).</p>
     *
     * @param id identifiant
     * @return la proposition
     * @throws NoSuchElementException si elle n'existe pas
     */
    public Proposition trouverParId(String id) {
        return propositionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Proposition introuvable : " + id));
    }

    /**
     * Toutes les propositions d'une campagne.
     *
     * @param campagneId identifiant de la campagne
     * @return les propositions, quel que soit leur statut
     */
    public List<Proposition> listerParCampagne(String campagneId) {
        return propositionRepository.findByCampagneId(campagneId);
    }

    /**
     * Propositions d'un fournisseur.
     *
     * @param fournisseurId identifiant du fournisseur
     * @return ses propositions, toutes campagnes confondues
     */
    public List<Proposition> listerParFournisseur(String fournisseurId) {
        return propositionRepository.findByFournisseurId(fournisseurId);
    }

    /**
     * Les paliers doivent partir de 0 L, se suivre sans trou ni chevauchement et finir sans limite haute :
     * ainsi, tout volume de campagne tombe dans un palier et un seul.
     */
    private List<Palier> verifierEtTrierPaliers(List<Palier> paliers) {
        if (paliers == null || paliers.isEmpty()) {
            throw new IllegalArgumentException("Au moins un palier est obligatoire");
        }

        List<Palier> tries = paliers.stream()
                .sorted(Comparator.comparingInt(Palier::getVolumeMin))
                .toList();

        if (tries.get(0).getVolumeMin() != 0) {
            throw new IllegalArgumentException("Le premier palier doit commencer à 0 litre");
        }

        for (int i = 0; i < tries.size(); i++) {
            Palier palier = tries.get(i);
            if (palier.getPrixLitre() == null || palier.getPrixLitre().signum() <= 0) {
                throw new IllegalArgumentException("Le prix au litre doit être strictement positif");
            }

            if (i == tries.size() - 1) {
                if (palier.getVolumeMax() != null) {
                    throw new IllegalArgumentException("Le dernier palier ne doit pas avoir de limite haute");
                }
            } else {
                if (palier.getVolumeMax() == null || palier.getVolumeMax() <= palier.getVolumeMin()) {
                    throw new IllegalArgumentException("Le volume max d'un palier doit dépasser son volume min");
                }
                if (palier.getVolumeMax() != tries.get(i + 1).getVolumeMin()) {
                    throw new IllegalArgumentException("Les paliers doivent se suivre sans trou ni chevauchement");
                }
            }
        }
        return new ArrayList<>(tries);
    }
}
