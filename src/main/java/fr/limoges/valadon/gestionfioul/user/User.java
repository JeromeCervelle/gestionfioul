package fr.limoges.valadon.gestionfioul.user;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Utilisateur de la plateforme : client, fournisseur ou administrateur selon son rôle
 * (document MongoDB de la collection {@code users}).
 *
 * <ul>
 *   <li>l'email est unique (index MongoDB) et enregistré en minuscules ;</li>
 *   <li>le mot de passe est stocké haché (BCrypt), jamais en clair ;</li>
 *   <li>l'adresse est celle de livraison pour un client, du siège pour un fournisseur ;</li>
 *   <li>la raison sociale ne concerne que les fournisseurs.</li>
 * </ul>
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String motDePasse;      // à stocker haché, jamais en clair
    private String nom;
    private String prenom;
    private String telephone;
    private Role role;
    private String adresse;         // livraison pour un client, siège pour un fournisseur
    private String raisonSociale;   // fournisseur uniquement, null pour un client

    public User() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }
}
