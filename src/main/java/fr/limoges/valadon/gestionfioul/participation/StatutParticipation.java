package fr.limoges.valadon.gestionfioul.participation;

/**
 * État d'une participation.
 */
public enum StatutParticipation {
    /** Le volume compte dans la commande groupée. */
    CONFIRMEE,
    /** Le client s'est retiré avant la date limite. */
    ANNULEE
}
