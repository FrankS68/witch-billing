package de.witchcafe.billing.client;

import java.util.Optional;
import java.util.OptionalLong;

import de.witchcafe.billing.client.api.BookingResponse;

/**
 * Zugang einer App zum zentralen Billing-Dienst. Wird per Auto-Configuration als Bean
 * bereitgestellt; ohne {@code witch-billing.url} ist Billing ausgeschaltet und alles erlaubt.
 * <p>
 * Typische Verwendung in einer App:
 * <pre>
 * try {
 *     billing.charge("ki-fragen", 20);
 *     erzeugeFragen(20);
 * } catch (InsufficientCreditException e) {
 *     Notification.show("Nicht genug Guthaben: " + e.getBalance() + " von " + e.getRequired());
 * }
 * </pre>
 * Die Varianten ohne userId nehmen den angemeldeten witch-auth-Benutzer und erzeugen
 * selbst einen Idempotenz-Schluessel.
 */
public interface BillingClient {

    /**
     * Verbrauch buchen.
     *
     * @return die Buchung, oder {@link BookingResponse#NOT_BOOKED}, wenn Billing aus ist oder
     *         der Dienst nicht erreichbar ist und fail-open gilt
     * @throws InsufficientCreditException  Guthaben reicht nicht; die Funktion nicht ausfuehren
     * @throws BillingUnavailableException  Dienst nicht erreichbar und fail-open=false
     * @throws BillingException             Feature unbekannt oder Anfrage ungueltig
     */
    BookingResponse charge(String userId, String feature, int quantity, String idempotencyKey);

    /** Beitrag gutschreiben. Ausnahmen wie bei {@link #charge(String, String, int, String)}, ohne 402. */
    BookingResponse reward(String userId, String feature, int quantity, String idempotencyKey);

    /** Kontostand; leer, wenn Billing aus ist oder der Dienst nicht erreichbar ist. */
    OptionalLong balance(String userId);

    /**
     * Verbrauch fuer den angemeldeten Benutzer buchen.
     *
     * @throws IllegalStateException wenn niemand angemeldet ist
     */
    default BookingResponse charge(String feature, int quantity) {
        return charge(requireCurrentUserId(), feature, quantity, IdempotencyKeys.next());
    }

    /** Einen Beitrag (Menge 1) fuer den angemeldeten Benutzer gutschreiben. */
    default BookingResponse reward(String feature) {
        return reward(requireCurrentUserId(), feature, 1, IdempotencyKeys.next());
    }

    /** Kontostand des angemeldeten Benutzers; leer auch, wenn niemand angemeldet ist. */
    default OptionalLong balance() {
        return currentUserId().map(this::balance).orElse(OptionalLong.empty());
    }

    /** witch-auth-UserId des angemeldeten Benutzers, falls witch-auth vorhanden und jemand angemeldet ist. */
    Optional<String> currentUserId();

    private String requireCurrentUserId() {
        return currentUserId().orElseThrow(() -> new IllegalStateException("Kein angemeldeter Benutzer"));
    }
}
