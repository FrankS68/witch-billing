package de.witchcafe.billing.client.api;

/**
 * Anfrage einer App an {@code POST /api/usage} (Verbrauch) oder
 * {@code POST /api/contributions} (Beitrag). Die App ergibt sich aus dem API-Key.
 *
 * @param userId         witch-auth-UserId ("provider:providerId")
 * @param feature        Feature laut Preisliste, z.B. "ki-fragen"
 * @param quantity       Menge, &gt; 0
 * @param idempotencyKey optional, max. 100 Zeichen; gleiche Schluessel werden nur einmal gebucht
 * @param userLabel      optional: lesbare Kennung des Benutzers fuer den Admin (E-Mail, ohne
 *                       E-Mail z.B. der Bluesky-Handle); der Dienst merkt sie sich am Konto
 * @param userName       optional: Anzeigename des Benutzers
 */
public record BookingRequest(String userId, String feature, int quantity, String idempotencyKey,
                             String userLabel, String userName) {

    /** Ohne Angaben zum Benutzer (wie bis 0.2.0). */
    public BookingRequest(String userId, String feature, int quantity, String idempotencyKey) {
        this(userId, feature, quantity, idempotencyKey, null, null);
    }
}
