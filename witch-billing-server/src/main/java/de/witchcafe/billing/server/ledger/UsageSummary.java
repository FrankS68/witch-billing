package de.witchcafe.billing.server.ledger;

/**
 * Nutzung eines Features in einem Zeitraum (Admin-Auswertung).
 *
 * @param bookings Anzahl Buchungen
 * @param quantity Summe der Mengen
 * @param credits  Summe der Credits mit Vorzeichen
 * @param users    Anzahl verschiedener Benutzer
 */
public record UsageSummary(String app, String feature, EntryKind kind,
                           Long bookings, Long quantity, Long credits, Long users) {
}
