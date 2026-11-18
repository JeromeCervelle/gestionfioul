package fr.limoges.valadon.gestionfioul.campagne;

/**
 * Étapes du cycle de vie d'une campagne.
 */
public enum StatutCampagne {
    /** Créée mais pas encore visible des clients. */
    BROUILLON,
    /** Les clients peuvent participer, les fournisseurs proposer. */
    OUVERTE,
    /** Date limite passée, {@code volumeTotal} figé. */
    CLOTUREE,
    /** Une proposition a été retenue. */
    ATTRIBUEE,
    /** La commande a été livrée : état final. */
    LIVREE,
    /** Campagne abandonnée : état final. */
    ANNULEE
}
