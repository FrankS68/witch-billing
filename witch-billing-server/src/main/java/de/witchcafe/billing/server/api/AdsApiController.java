package de.witchcafe.billing.server.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.witchcafe.billing.client.api.AdResponse;
import de.witchcafe.billing.client.api.AdsResponse;
import de.witchcafe.billing.server.ads.AdService;
import de.witchcafe.billing.server.ads.BillingAd;

/**
 * REST-API: Werbung fuer einen Benutzer in der App des API-Keys ({@link AppKeyFilter}),
 * zusammen mit Kontostand und Schulden-Status.
 */
@RestController
@RequestMapping("/api")
public class AdsApiController {

    private final AdService ads;

    public AdsApiController(AdService ads) {
        this.ads = ads;
    }

    @GetMapping("/ads/{userId}")
    public AdsResponse ads(@PathVariable String userId, Authentication app) {
        AdService.UserAds result = ads.adsFor(app.getName(), userId);
        return new AdsResponse(userId, result.balance(), result.inDebt(),
                result.ads().stream().map(AdsApiController::toResponse).toList());
    }

    private static AdResponse toResponse(BillingAd ad) {
        return new AdResponse(ad.getId() == null ? 0 : ad.getId(), ad.getKind().name(), ad.getTitle(), ad.getText(),
                ad.getImageUrl(), ad.getLinkUrl(), ad.getAdsenseSlot());
    }
}
