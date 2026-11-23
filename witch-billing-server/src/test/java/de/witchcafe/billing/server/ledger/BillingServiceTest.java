package de.witchcafe.billing.server.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(BillingService.class)
// allow-debt ausdruecklich aus: application.properties schaltet es fuer den Betrieb ein
@TestPropertySource(properties = {"witch-billing.welcome-credits=100", "witch-billing.allow-debt=false"})
class BillingServiceTest {

    private static final String USER = "github:4711";

    @Autowired BillingService billing;
    @Autowired BillingPriceRepository prices;
    @Autowired BillingEntryRepository entries;

    @BeforeEach
    void priceList() {
        prices.save(new BillingPrice("quiz", "ki-fragen", PriceKind.COST, 2, "KI-Frage erzeugen"));
        prices.save(new BillingPrice("quiz", "frage-eingereicht", PriceKind.REWARD, 5, "Frage eingereicht"));
        prices.save(new BillingPrice("quiz", "spiel", PriceKind.COST, 0, "Spiel gestartet (nur zaehlen)"));
    }

    @Test
    void neuerBenutzerBekommtStartguthaben() {
        assertThat(billing.balance(USER)).isEqualTo(100);

        Booking booking = billing.charge("quiz", USER, "ki-fragen", 10, null);

        assertThat(booking.credits()).isEqualTo(-20);
        assertThat(booking.balanceAfter()).isEqualTo(80);
        assertThat(billing.recentEntries(USER, 10))
                .extracting(BillingEntry::getKind)
                .containsExactly(EntryKind.USAGE, EntryKind.WELCOME);
    }

    @Test
    void verbrauchOhneGenugGuthabenWirdAbgelehntUndNichtGebucht() {
        billing.charge("quiz", USER, "ki-fragen", 50, null); // 100 -> 0

        assertThatThrownBy(() -> billing.charge("quiz", USER, "ki-fragen", 1, null))
                .isInstanceOf(InsufficientCreditsException.class)
                .satisfies(e -> {
                    var ex = (InsufficientCreditsException) e;
                    assertThat(ex.getBalance()).isZero();
                    assertThat(ex.getRequired()).isEqualTo(2);
                });
        assertThat(billing.balance(USER)).isZero();
        assertThat(billing.recentEntries(USER, 10)).hasSize(2);
    }

    @Test
    void beitragFuelltGuthabenAuf() {
        billing.charge("quiz", USER, "ki-fragen", 50, null);

        Booking booking = billing.reward("quiz", USER, "frage-eingereicht", 3, null);

        assertThat(booking.credits()).isEqualTo(15);
        assertThat(billing.balance(USER)).isEqualTo(15);
    }

    @Test
    void preisNullWirdNurGezaehlt() {
        Booking booking = billing.charge("quiz", USER, "spiel", 1, null);

        assertThat(booking.credits()).isZero();
        assertThat(billing.balance(USER)).isEqualTo(100);
        assertThat(billing.recentEntries(USER, 10)).first()
                .extracting(BillingEntry::getFeature).isEqualTo("spiel");
    }

    @Test
    void gleicherIdempotenzSchluesselBuchtNurEinmal() {
        Booking first = billing.charge("quiz", USER, "ki-fragen", 5, "req-1");
        Booking again = billing.charge("quiz", USER, "ki-fragen", 5, "req-1");

        assertThat(first.replay()).isFalse();
        assertThat(again.replay()).isTrue();
        assertThat(again.entryId()).isEqualTo(first.entryId());
        assertThat(billing.balance(USER)).isEqualTo(90);
    }

    @Test
    void idempotenzSchluesselGiltProApp() {
        prices.save(new BillingPrice("games", "ki-gegner", PriceKind.COST, 1, "KI-Gegner"));

        billing.charge("quiz", USER, "ki-fragen", 1, "req-1");
        Booking games = billing.charge("games", USER, "ki-gegner", 1, "req-1");

        assertThat(games.replay()).isFalse();
        assertThat(billing.balance(USER)).isEqualTo(97);
    }

    @Test
    void unbekanntesOderInaktivesFeatureWirdAbgelehnt() {
        assertThatThrownBy(() -> billing.charge("quiz", USER, "gibt-es-nicht", 1, null))
                .isInstanceOf(UnknownFeatureException.class);
        // Belohnung darf nicht als Verbrauch gebucht werden und umgekehrt
        assertThatThrownBy(() -> billing.charge("quiz", USER, "frage-eingereicht", 1, null))
                .isInstanceOf(UnknownFeatureException.class);
        assertThatThrownBy(() -> billing.reward("quiz", USER, "ki-fragen", 1, null))
                .isInstanceOf(UnknownFeatureException.class);

        prices.findByAppAndFeature("quiz", "ki-fragen").orElseThrow().setActive(false);
        assertThatThrownBy(() -> billing.charge("quiz", USER, "ki-fragen", 1, null))
                .isInstanceOf(UnknownFeatureException.class);

        assertThat(entries.findAll()).isEmpty();
    }

    @Test
    void adminBuchtGutschriftUndKorrektur() {
        Booking grant = billing.adminBooking(USER, 50, "Fragen Korrektur gelesen", "google:admin");
        Booking correction = billing.adminBooking(USER, -200, "Fehlbuchung", "google:admin");

        assertThat(grant.balanceAfter()).isEqualTo(150);
        assertThat(correction.balanceAfter()).isEqualTo(-50);
        List<BillingEntry> list = billing.recentEntries(USER, 10);
        assertThat(list).extracting(BillingEntry::getKind)
                .containsExactly(EntryKind.CORRECTION, EntryKind.ADMIN_GRANT, EntryKind.WELCOME);
        assertThat(list.get(0).getBookedBy()).isEqualTo("google:admin");
        assertThat(list.get(0).getApp()).isEqualTo(BillingService.ADMIN_APP);
    }

    @Test
    void ungueltigeEingabenWerdenAbgelehnt() {
        assertThatThrownBy(() -> billing.charge("quiz", USER, "ki-fragen", 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billing.charge("quiz", " ", "ki-fragen", 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billing.charge("quiz", USER, "ki-fragen", 1, "x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billing.adminBooking(USER, 0, "nichts", "google:admin"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BillingPrice("quiz", "x", PriceKind.COST, -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminBuchungVerlangtUserIdMitProvider() {
        assertThatThrownBy(() -> billing.adminBooking("4711", 10, "ohne Provider", "google:admin"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billing.adminBooking("github: 4711", 10, "Leerzeichen", "google:admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kontenSuchen() {
        billing.charge("quiz", "github:1", "spiel", 1, null);
        billing.charge("quiz", "google:2", "spiel", 1, null);
        billing.charge("quiz", "github:3", "spiel", 1, null);

        assertThat(billing.accounts("GITHUB", 10)).extracting(BillingAccount::getUserId)
                .containsExactly("github:1", "github:3");
        assertThat(billing.accounts("", 10)).hasSize(3);
        assertThat(billing.accounts(null, 2)).hasSize(2);
    }

    @Test
    void nutzungWirdProFeatureSummiert() {
        billing.charge("quiz", "github:1", "ki-fragen", 2, null);
        billing.charge("quiz", "github:1", "ki-fragen", 3, null);
        billing.charge("quiz", "github:2", "ki-fragen", 1, null);
        billing.reward("quiz", "github:2", "frage-eingereicht", 1, null);
        billing.adminBooking("github:2", 50, "Hilfe", "google:admin");

        var summary = billing.usageSince(java.time.Instant.now().minusSeconds(60));

        assertThat(summary).containsExactly(
                new UsageSummary("quiz", "frage-eingereicht", EntryKind.CONTRIBUTION, 1L, 1L, 5L, 1L),
                new UsageSummary("quiz", "ki-fragen", EntryKind.USAGE, 3L, 6L, -12L, 2L));
        assertThat(billing.usageSince(java.time.Instant.now().plusSeconds(60))).isEmpty();
    }

    @Test
    void preisAnlegenUndAendern() {
        BillingPrice price = billing.createPrice(" Games ", "KI-Gegner", PriceKind.COST, 3, "KI-Gegner pro Zug");

        assertThat(price.getApp()).isEqualTo("games");
        assertThat(price.getFeature()).isEqualTo("ki-gegner");
        assertThat(billing.charge("games", USER, "ki-gegner", 1, null).credits()).isEqualTo(-3);

        billing.updatePrice(price.getId(), 5, "teurer", true);
        assertThat(billing.charge("games", USER, "ki-gegner", 1, null).credits()).isEqualTo(-5);

        billing.updatePrice(price.getId(), 5, "aus", false);
        assertThatThrownBy(() -> billing.charge("games", USER, "ki-gegner", 1, null))
                .isInstanceOf(UnknownFeatureException.class);
    }

    @Test
    void ungueltigerOderDoppelterPreisWirdAbgelehnt() {
        assertThatThrownBy(() -> billing.createPrice("quiz", "ki-fragen", PriceKind.COST, 1, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("schon");
        assertThatThrownBy(() -> billing.createPrice("quiz", "mit leerzeichen", PriceKind.COST, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billing.createPrice("admin", "x", PriceKind.REWARD, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> billing.updatePrice(99999, 1, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
