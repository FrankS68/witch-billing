package de.witchcafe.billing.client.api;

/**
 * Anfrage einer App an {@code POST /api/usage} (Verbrauch) oder
 * {@code POST /api/contributions} (Beitrag). Die App ergibt sich aus dem API-Key.
 *
 * @param userId         witch-auth-UserId ("provider:providerId")
 * @param feature        Feature laut Preisliste, z.B. "ki-fragen"
 * @param quantity       Menge, &gt; 0
 * @param idempotencyKey optional, max. 100 Zeichen; gleiche Schluessel werden nur einmal gebucht
 */
public record BookingRequest(String userId, String feature, int quantity, String idempotencyKey) {
}
