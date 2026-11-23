package de.witchcafe.billing.vaadin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.witchcafe.billing.client.BillingClient;
import de.witchcafe.billing.client.api.AdResponse;
import de.witchcafe.billing.client.api.AdsResponse;
import de.witchcafe.billing.client.api.BookingResponse;

class BillingAdsTest {

    private static final String USER = "github:4711";

    private static final AdResponse HOUSE = new AdResponse(1, AdResponse.HOUSE, "Probier den Garten", null,
            null, "https://garten.example", null);
    private static final AdResponse ADSENSE = new AdResponse(2, AdResponse.ADSENSE, null, null, null, null, "123");
    private static final AdResponse LEER = new AdResponse(3, AdResponse.HOUSE, " ", null, null, null, null);

    /** Zaehlt die Abfragen beim Dienst; Kontostand einstellbar. */
    private static final class FakeClient implements BillingClient {
        final AtomicInteger calls = new AtomicInteger();
        long balance = -5;

        @Override public BookingResponse charge(String u, String f, int q, String k) { return BookingResponse.NOT_BOOKED; }
        @Override public BookingResponse reward(String u, String f, int q, String k) { return BookingResponse.NOT_BOOKED; }
        @Override public OptionalLong balance(String u) { return OptionalLong.of(balance); }
        @Override public Optional<String> currentUserId() { return Optional.of(USER); }

        @Override
        public Optional<AdsResponse> ads(String userId) {
            calls.incrementAndGet();
            return Optional.of(new AdsResponse(userId, balance, balance < 0, List.of(HOUSE, ADSENSE, LEER)));
        }
    }

    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-23T12:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static AdsProperties props(String adsenseClient) {
        return new AdsProperties(null, null, null, Duration.ofSeconds(60), adsenseClient, null);
    }

    @Test
    void antwortWirdZwischengespeichert() {
        FakeClient client = new FakeClient();
        MutableClock clock = new MutableClock();
        BillingAds ads = new BillingAds(client, props(null), null, clock);

        assertThat(ads.isInDebt()).isTrue();
        assertThat(ads.current()).isPresent();
        assertThat(client.calls).hasValue(1);

        clock.now = clock.now.plusSeconds(61);
        ads.current();
        assertThat(client.calls).hasValue(2);
    }

    @Test
    void nachBuchungWirdNeuGefragt() {
        FakeClient client = new FakeClient();
        BillingAds ads = new BillingAds(client, props(null), null, new MutableClock());
        ads.current();

        client.balance = 3;
        ads.afterReward(new BookingResponse(7, 8, 3, false));   // ohne UI: nur Cache leeren

        assertThat(ads.isInDebt()).isFalse();
        assertThat(client.calls).hasValue(2);
    }

    @Test
    void nichtGebuchtAendertNichts() {
        FakeClient client = new FakeClient();
        BillingAds ads = new BillingAds(client, props(null), null, new MutableClock());
        ads.current();

        ads.afterCharge(BookingResponse.NOT_BOOKED);
        ads.current();

        assertThat(client.calls).hasValue(1);
    }

    @Test
    void adsenseNurMitPublisherIdUndLeereEintraegeNie() {
        AdsResponse response = new AdsResponse(USER, -5, true, List.of(HOUSE, ADSENSE, LEER));

        assertThat(BillingAds.displayable(response, false)).containsExactly(HOUSE);
        assertThat(BillingAds.displayable(response, true)).containsExactly(HOUSE, ADSENSE);
        assertThat(props("ca-pub-1234567890").adsenseEnabled()).isTrue();
        assertThat(props("irgendwas").adsenseEnabled()).isFalse();
    }
}
