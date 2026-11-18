package fr.limoges.valadon.gestionfioul.user;

/**
 * Rôle d'un utilisateur, qui détermine les routes auxquelles il a droit.
 */
public enum Role {
    /** Organise les campagnes ; jamais attribuable à l'inscription (voir {@link fr.limoges.valadon.gestionfioul.security.AdminInitializer AdminInitializer}). */
    ADMIN,
    /** Participe aux campagnes en s'engageant sur un volume. */
    CLIENT,
    /** Propose des barèmes de prix. */
    FOURNISSEUR
}
