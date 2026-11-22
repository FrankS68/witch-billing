package de.witchcafe.billing.server.ledger;

/**
 * Art einer Buchung im Journal.
 */
public enum EntryKind {
    /** Startguthaben beim ersten Kontakt eines Benutzers mit dem Dienst. */
    WELCOME,
    /** Verbrauch einer kostenpflichtigen Funktion in einer App (negativ). */
    USAGE,
    /** Gutschrift fuer einen Beitrag des Benutzers in einer App (positiv). */
    CONTRIBUTION,
    /** Manuelle Gutschrift durch einen Admin, z.B. fuer eine erbrachte Dienstleistung. */
    ADMIN_GRANT,
    /** Manuelle Korrektur durch einen Admin (meist negativ). */
    CORRECTION
}
