package de.witchcafe.billing.client;

/**
 * Der Billing-Dienst ist nicht erreichbar, antwortet mit einem Serverfehler oder lehnt den
 * API-Key ab. Kommt nur bei {@code witch-billing.fail-open=false}; sonst wird die Aktion
 * erlaubt und eine Warnung geloggt.
 */
public class BillingUnavailableException extends BillingException {

    public static final String CODE = "UNAVAILABLE";

    public BillingUnavailableException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
