package de.witchcafe.billing.server.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import de.witchcafe.billing.server.ledger.BillingPrice;
import de.witchcafe.billing.server.ledger.BillingPriceRepository;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.billing.server.ledger.PriceKind;

@DataJpaTest
@Import({AdService.class, BillingService.class})
@TestPropertySource(properties = {"witch-billing.welcome-credits=10", "witch-billing.allow-debt=true"})
class AdServiceTest {

    private static final String USER = "github:4711";

    @Autowired AdService adService;
    @Autowired BillingService billing;
    @Autowired BillingPriceRepository prices;

    @BeforeEach
    void setUp() {
        prices.save(new BillingPrice("garten", "beet-angelegt", PriceKind.COST, 40, null));
        adService.save(house("Probier das Quiz", null, AdAudience.DEBTORS));
        adService.save(house("Nur im Garten", "garten", AdAudience.DEBTORS));
        adService.save(house("Nur im Quiz", "quiz", AdAudience.DEBTORS));
        adService.save(house("Passend dazu", "garten", AdAudience.ALL));
        BillingAd off = house("Abgeschaltet", null, AdAudience.DEBTORS);
        off.setActive(false);
        adService.save(off);
    }

    @Test
    void imPlusNurWerbungFuerAlle() {
        AdService.UserAds ads = adService.adsFor("garten", USER);

        assertThat(ads.inDebt()).isFalse();
        assertThat(ads.balance()).isEqualTo(10);
        assertThat(ads.ads()).extracting(BillingAd::getTitle).containsExactly("Passend dazu");
    }

    @Test
    void imMinusAlleAktivenEintraegeDerApp() {
        billing.charge("garten", USER, "beet-angelegt", 1, null); // 10 -> -30

        AdService.UserAds ads = adService.adsFor("garten", USER);

        assertThat(ads.inDebt()).isTrue();
        assertThat(ads.balance()).isEqualTo(-30);
        assertThat(ads.ads()).extracting(BillingAd::getTitle)
                .containsExactly("Probier das Quiz", "Nur im Garten", "Passend dazu");
    }

    @Test
    void ungueltigeEintraegeWerdenAbgelehnt() {
        BillingAd amazonOhneLink = new BillingAd(AdKind.AMAZON, AdAudience.DEBTORS);
        amazonOhneLink.setTitle("Saatgut");
        assertThatThrownBy(() -> adService.save(amazonOhneLink)).hasMessageContaining("Link");

        BillingAd javascriptLink = house("Klick", null, AdAudience.DEBTORS);
        javascriptLink.setLinkUrl("javascript:alert(1)");
        assertThatThrownBy(() -> adService.save(javascriptLink)).hasMessageContaining("https");

        BillingAd adsense = new BillingAd(AdKind.ADSENSE, AdAudience.DEBTORS);
        adsense.setAdsenseSlot("abc");
        assertThatThrownBy(() -> adService.save(adsense)).hasMessageContaining("Ziffern");
    }

    private static BillingAd house(String title, String app, AdAudience audience) {
        BillingAd ad = new BillingAd(AdKind.HOUSE, audience);
        ad.setTitle(title);
        ad.setApp(app);
        ad.setLinkUrl("https://quiz.segieth.cologne");
        return ad;
    }
}
