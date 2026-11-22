package de.witchcafe.billing.server.ledger;

/**
 * Ob ein Eintrag der Preisliste Guthaben kostet oder einbringt.
 */
public enum PriceKind {
    /** Verbrauch: die App bucht ueber {@code charge}, Credits werden abgezogen. */
    COST,
    /** Beitrag: die App bucht ueber {@code reward}, Credits werden gutgeschrieben. */
    REWARD
}
