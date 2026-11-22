package de.witchcafe.billing.client;

/**
 * Buchung nicht moeglich, z.B. weil das Feature nicht in der Preisliste steht (HTTP 404)
 * oder die Anfrage ungueltig ist (HTTP 400). Meist ein Fehler in App oder Preisliste.
 */
public class BillingException extends RuntimeException {

    private final String code;

    public BillingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BillingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** Fehlercode des Dienstes, z.B. "UNKNOWN_FEATURE", oder "UNAVAILABLE". */
    public String getCode() {
        return code;
    }
}
