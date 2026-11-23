package de.witchcafe.billing.vaadin;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.vaadin.flow.component.UI;

import de.witchcafe.billing.client.BillingClient;
import de.witchcafe.billing.client.api.AdResponse;
import de.witchcafe.billing.client.api.AdsResponse;
import de.witchcafe.billing.client.api.BookingResponse;

/**
 * Werbung fuer den angemeldeten Benutzer: fragt den Billing-Dienst (mit Zwischenspeicher pro
 * Benutzer, siehe {@code witch-billing.ads.cache}) und zeigt nach einer Buchung ins Minus die
 * Einblendung ({@link AdBreak}).
 * <p>
 * Verwendung in einer App nach einer Buchung:
 * <pre>
 * BookingResponse booking = billing.charge("partie", 1);
 * ads.afterCharge(booking);   // im Minus: kurze Werbe-Einblendung, sonst nichts
 * </pre>
 */
public class BillingAds {

    private record Cached(AdsResponse response, Instant until) {}

    private final BillingClient billing;
    private final AdsProperties properties;
    private final String creditsUrl;
    private final Clock clock;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public BillingAds(BillingClient billing, AdsProperties properties, String creditsUrl, Clock clock) {
        this.billing    = billing;
        this.properties = properties;
        this.creditsUrl = creditsUrl;
        this.clock      = clock;
    }

    /** Werbung und Schulden-Status des angemeldeten Benutzers; leer, wenn anonym oder Billing aus. */
    public Optional<AdsResponse> current() {
        return billing.currentUserId().flatMap(this::forUser);
    }

    /** true, wenn das Konto des angemeldeten Benutzers im Minus ist. */
    public boolean isInDebt() {
        return current().map(AdsResponse::inDebt).orElse(false);
    }

    Optional<AdsResponse> forUser(String userId) {
        Instant now = clock.instant();
        Cached cached = cache.get(userId);
        if (cached != null && now.isBefore(cached.until())) {
            return Optional.of(cached.response());
        }
        Optional<AdsResponse> fresh = billing.ads(userId);
        cache.values().removeIf(c -> !now.isBefore(c.until()));
        fresh.ifPresent(r -> cache.put(userId, new Cached(r, now.plus(properties.cacheOrDefault()))));
        return fresh;
    }

    /**
     * Nach einer Buchung aufrufen: der Kontostand hat sich geaendert, also neu fragen; ist das
     * Konto danach im Minus, erscheint (in einer Vaadin-Anfrage) die Werbe-Einblendung.
     */
    public void afterCharge(BookingResponse booking) {
        if (booking == null || !booking.isBooked()) {
            return;
        }
        billing.currentUserId().ifPresent(cache::remove);
        if (booking.isInDebt() && properties.adBreakEnabled() && UI.getCurrent() != null) {
            AdBreak.show(this, booking.balanceAfter());
        }
    }

    /** Nach einer Gutschrift aufrufen, damit das Banner verschwindet, sobald das Konto im Plus ist. */
    public void afterReward(BookingResponse booking) {
        if (booking != null && booking.isBooked()) {
            billing.currentUserId().ifPresent(cache::remove);
        }
    }

    /** Eine anzeigbare Werbung zufaellig auswaehlen (AdSense nur mit Publisher-Id). */
    public Optional<AdResponse> pick(AdsResponse response) {
        List<AdResponse> displayable = displayable(response, properties.adsenseEnabled());
        if (displayable.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(displayable.get(ThreadLocalRandom.current().nextInt(displayable.size())));
    }

    static List<AdResponse> displayable(AdsResponse response, boolean adsenseEnabled) {
        return response.ads().stream()
                .filter(ad -> !AdResponse.ADSENSE.equals(ad.kind()) || adsenseEnabled)
                .filter(ad -> AdResponse.ADSENSE.equals(ad.kind()) || hasText(ad.title()) || hasText(ad.text()))
                .toList();
    }

    AdsProperties properties() {
        return properties;
    }

    /** Link zur Guthaben-Seite des Billing-Dienstes, kann leer sein. */
    String creditsUrl() {
        return creditsUrl;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
