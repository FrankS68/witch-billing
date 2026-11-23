package de.witchcafe.billing.vaadin;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Einstellungen fuer die Werbung (application.properties), alle optional:
 * <pre>
 * witch-billing.ads.banner=true            # Banner unten fuer Benutzer im Minus
 * witch-billing.ads.ad-break=true          # Einblendung beim Start einer Partie im Minus
 * witch-billing.ads.ad-break-seconds=5
 * witch-billing.ads.cache=60s              # so lange gilt die Antwort des Dienstes pro Benutzer
 * witch-billing.ads.adsense-client=        # ca-pub-...; leer = AdSense-Eintraege werden uebersprungen
 * witch-billing.ads.credits-url=           # Link "Guthaben ansehen"; leer = witch-billing.url
 * </pre>
 */
@ConfigurationProperties("witch-billing.ads")
public record AdsProperties(Boolean banner, Boolean adBreak, Integer adBreakSeconds, Duration cache,
                            String adsenseClient, String creditsUrl) {

    public boolean bannerEnabled() {
        return banner == null || banner;
    }

    public boolean adBreakEnabled() {
        return adBreak == null || adBreak;
    }

    public int adBreakSecondsOrDefault() {
        return adBreakSeconds == null ? 5 : Math.max(0, adBreakSeconds);
    }

    public Duration cacheOrDefault() {
        return cache == null ? Duration.ofSeconds(60) : cache;
    }

    public boolean adsenseEnabled() {
        return adsenseClient != null && adsenseClient.matches("ca-pub-[0-9]{5,20}");
    }
}
