package de.witchcafe.billing.server.ledger;

/**
 * Fuer App und Feature gibt es keinen aktiven, passenden Eintrag in der Preisliste.
 */
public class UnknownFeatureException extends RuntimeException {

    public UnknownFeatureException(String app, String feature, PriceKind expected) {
        super("Kein aktiver Preis (" + expected + ") fuer " + app + "/" + feature);
    }
}
