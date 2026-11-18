package fr.limoges.valadon.gestionfioul.proposition;

/**
 * État d'une proposition.
 */
public enum StatutProposition {
    /** En attente de la clôture et de l'attribution. */
    SOUMISE,
    /** La moins-disante : elle a gagné. */
    RETENUE,
    /** Une autre proposition a été retenue. */
    REJETEE
}
