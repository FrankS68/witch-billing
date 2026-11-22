package de.witchcafe.billing.server.ledger;

/**
 * Ergebnis einer Buchung.
 *
 * @param entryId      Id der Buchung im Journal
 * @param credits      gebuchter Betrag mit Vorzeichen
 * @param balanceAfter Kontostand nach der Buchung
 * @param replay       true, wenn die Buchung schon vorher mit demselben Idempotenz-Schluessel
 *                     angelegt worden war und nur das alte Ergebnis zurueckgegeben wird
 */
public record Booking(long entryId, long credits, long balanceAfter, boolean replay) {

    static Booking of(BillingEntry entry, boolean replay) {
        return new Booking(entry.getId(), entry.getCredits(), entry.getBalanceAfter(), replay);
    }
}
