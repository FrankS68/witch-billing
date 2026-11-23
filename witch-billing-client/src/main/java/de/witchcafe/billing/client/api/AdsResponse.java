package de.witchcafe.billing.client.api;

import java.util.List;

/**
 * Werbung fuer einen Benutzer in einer App, zusammen mit seinem Kontostand.
 *
 * @param userId  Benutzer
 * @param balance Kontostand
 * @param inDebt  true, wenn das Konto im Minus ist; dann soll die App Werbung zeigen
 * @param ads     passende aktive Werbung: im Minus alle fuer diese App, sonst nur die
 *                Eintraege fuer alle Benutzer (z.B. "Passend dazu"-Empfehlungen)
 */
public record AdsResponse(String userId, long balance, boolean inDebt, List<AdResponse> ads) {

    public AdsResponse {
        ads = ads == null ? List.of() : List.copyOf(ads);
    }
}
