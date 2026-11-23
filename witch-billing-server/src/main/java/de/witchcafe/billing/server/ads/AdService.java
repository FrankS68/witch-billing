package de.witchcafe.billing.server.ads;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.witchcafe.billing.server.ledger.BillingService;

/**
 * Werbung pflegen und fuer einen Benutzer auswaehlen.
 * <p>
 * Benutzer im Minus bekommen alle aktiven Eintraege ihrer App ({@link AdAudience#DEBTORS} und
 * {@link AdAudience#ALL}), alle anderen nur die fuer {@link AdAudience#ALL}.
 */
@Service
@Transactional
public class AdService {

    /** Ergebnis von {@link #adsFor(String, String)}. */
    public record UserAds(long balance, boolean inDebt, List<BillingAd> ads) {}

    private final BillingAdRepository ads;
    private final BillingService billing;

    public AdService(BillingAdRepository ads, BillingService billing) {
        this.ads     = ads;
        this.billing = billing;
    }

    /** Kontostand, Schulden-Status und passende Werbung eines Benutzers in einer App. */
    @Transactional(readOnly = true)
    public UserAds adsFor(String app, String userId) {
        long balance = billing.balance(userId);
        boolean inDebt = balance < 0;
        List<BillingAd> matching = ads.findByActiveTrueOrderByIdAsc().stream()
                .filter(ad -> ad.appliesTo(app))
                .filter(ad -> inDebt || ad.getAudience() == AdAudience.ALL)
                .toList();
        return new UserAds(balance, inDebt, matching);
    }

    @Transactional(readOnly = true)
    public List<BillingAd> all() {
        return ads.findAllByOrderByIdAsc();
    }

    /**
     * Neue Werbung anlegen oder bestehende speichern, nach Pruefung der Felder.
     *
     * @throws IllegalArgumentException bei unvollstaendigen oder ungueltigen Angaben
     */
    public BillingAd save(BillingAd ad) {
        ad.setApp(trimToNull(ad.getApp()) == null ? null : ad.getApp().trim().toLowerCase(Locale.ROOT));
        ad.setTitle(trimToNull(ad.getTitle()));
        ad.setText(trimToNull(ad.getText()));
        ad.setImageUrl(trimToNull(ad.getImageUrl()));
        ad.setLinkUrl(trimToNull(ad.getLinkUrl()));
        ad.setAdsenseSlot(trimToNull(ad.getAdsenseSlot()));
        validate(ad);
        return ads.save(ad);
    }

    static void validate(BillingAd ad) {
        if (ad.getKind() == null || ad.getAudience() == null) {
            throw new IllegalArgumentException("Art und Zielgruppe angeben");
        }
        if (ad.getApp() != null && !ad.getApp().matches("[a-z0-9._-]{1,50}")) {
            throw new IllegalArgumentException("App darf nur a-z, 0-9, Punkt, Minus und Unterstrich enthalten");
        }
        requireHttp(ad.getImageUrl(), "Bild-URL");
        requireHttp(ad.getLinkUrl(), "Link");
        switch (ad.getKind()) {
            case HOUSE, AMAZON -> {
                if (ad.getTitle() == null && ad.getText() == null) {
                    throw new IllegalArgumentException("Titel oder Text angeben");
                }
                if (ad.getLinkUrl() == null && ad.getKind() == AdKind.AMAZON) {
                    throw new IllegalArgumentException("Amazon-Werbung braucht einen Link (mit Partner-ID)");
                }
            }
            case ADSENSE -> {
                if (ad.getAdsenseSlot() == null || !ad.getAdsenseSlot().matches("\\d{1,20}")) {
                    throw new IllegalArgumentException("AdSense braucht die Anzeigenblock-Id (nur Ziffern)");
                }
            }
        }
    }

    private static void requireHttp(String url, String label) {
        if (url == null) {
            return;
        }
        try {
            String scheme = URI.create(url).getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException(label + " muss mit https:// beginnen");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(label)) {
                throw e;
            }
            throw new IllegalArgumentException(label + " ist keine gueltige URL");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
