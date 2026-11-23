package de.witchcafe.billing.server.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** witch-billing.allow-debt=true: Verbrauch wird auch ohne Guthaben gebucht. */
@DataJpaTest
@Import(BillingService.class)
@TestPropertySource(properties = {"witch-billing.welcome-credits=10", "witch-billing.allow-debt=true"})
class BillingServiceDebtTest {

    private static final String USER = "github:4711";

    @Autowired BillingService billing;
    @Autowired BillingPriceRepository prices;

    @BeforeEach
    void priceList() {
        prices.save(new BillingPrice("garten", "beet-angelegt", PriceKind.COST, 40, null));
        prices.save(new BillingPrice("garten", "pflanze-angelegt", PriceKind.REWARD, 1, null));
    }

    @Test
    void verbrauchOhneGuthabenGehtInsMinus() {
        assertThat(billing.isAllowDebt()).isTrue();

        Booking booking = billing.charge("garten", USER, "beet-angelegt", 1, null); // 10 -> -30

        assertThat(booking.credits()).isEqualTo(-40);
        assertThat(booking.balanceAfter()).isEqualTo(-30);
        assertThat(billing.balance(USER)).isEqualTo(-30);
    }

    @Test
    void beitraegeBauenSchuldenAb() {
        billing.charge("garten", USER, "beet-angelegt", 1, null);           // -30
        Booking booking = billing.reward("garten", USER, "pflanze-angelegt", 31, null);

        assertThat(booking.balanceAfter()).isEqualTo(1);
    }
}
