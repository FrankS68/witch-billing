package de.witchcafe.billing.client.api;

/**
 * Fehlerantwort des Dienstes.
 *
 * @param code     z.B. "INSUFFICIENT_CREDITS" (HTTP 402), "UNKNOWN_FEATURE" (404), "INVALID_REQUEST" (400)
 * @param message  lesbare Beschreibung
 * @param balance  bei INSUFFICIENT_CREDITS: aktueller Kontostand, sonst null
 * @param required bei INSUFFICIENT_CREDITS: benoetigter Betrag, sonst null
 */
public record ErrorResponse(String code, String message, Long balance, Long required) {

    public static final String INSUFFICIENT_CREDITS = "INSUFFICIENT_CREDITS";
    public static final String UNKNOWN_FEATURE      = "UNKNOWN_FEATURE";
    public static final String INVALID_REQUEST      = "INVALID_REQUEST";
}
