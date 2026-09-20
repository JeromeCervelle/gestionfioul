package fr.limoges.valadon.gestionfioul.campagne;

public enum StatutCampagne {
    BROUILLON,   // créée mais pas encore visible des clients
    OUVERTE,     // les clients peuvent participer, les fournisseurs proposer
    CLOTUREE,    // date limite passée, volumeTotal figé
    ATTRIBUEE,   // une proposition a été retenue
    LIVREE,
    ANNULEE
}
