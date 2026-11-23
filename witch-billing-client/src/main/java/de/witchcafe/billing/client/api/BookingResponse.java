package de.witchcafe.billing.client.api;

/**
 * Antwort auf eine erfolgreiche Buchung.
 *
 * @param entryId      Id der Buchung im Journal; 0 = nicht gebucht (siehe {@link #NOT_BOOKED})
 * @param credits      gebuchter Betrag mit Vorzeichen (Verbrauch negativ)
 * @param balanceAfter Kontostand nach der Buchung
 * @param replay       true, wenn mit diesem Idempotenz-Schluessel schon gebucht worden war
 */
public record BookingResponse(long entryId, long credits, long balanceAfter, boolean replay) {

    /**
     * Ergebnis des Clients, wenn nicht gebucht wurde und die Aktion trotzdem erlaubt ist:
     * Billing ausgeschaltet oder Dienst nicht erreichbar bei {@code witch-billing.fail-open=true}.
     * Der Kontostand ist dann unbekannt.
     */
    public static final BookingResponse NOT_BOOKED = new BookingResponse(0, 0, 0, false);

    public boolean isBooked() {
        return entryId != 0;
    }

    /** true, wenn gebucht wurde und das Konto danach im Minus ist (die App soll Werbung zeigen). */
    public boolean isInDebt() {
        return isBooked() && balanceAfter < 0;
    }
}
