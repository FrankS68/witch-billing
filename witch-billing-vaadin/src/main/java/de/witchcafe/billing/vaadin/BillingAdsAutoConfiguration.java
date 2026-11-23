package de.witchcafe.billing.vaadin;

import java.time.Clock;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import de.witchcafe.billing.client.BillingAutoConfiguration;
import de.witchcafe.billing.client.BillingClient;
import de.witchcafe.billing.client.BillingProperties;

/**
 * Werbung fuer Benutzer im Minus: {@link BillingAds} und, sofern nicht mit
 * {@code witch-billing.ads.banner=false} abgeschaltet, das Banner auf jeder Seite.
 * Ohne {@code witch-billing.url} liefert der Client keine Werbung, dann bleibt alles unsichtbar.
 */
@AutoConfiguration(after = BillingAutoConfiguration.class)
@ConditionalOnClass(name = "com.vaadin.flow.server.VaadinServiceInitListener")
@ConditionalOnBean(BillingClient.class)
@EnableConfigurationProperties(AdsProperties.class)
public class BillingAdsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    BillingAds billingAds(BillingClient billing, AdsProperties ads, BillingProperties billingProperties) {
        String creditsUrl = ads.creditsUrl() != null && !ads.creditsUrl().isBlank()
                ? ads.creditsUrl() : billingProperties.url();
        return new BillingAds(billing, ads, creditsUrl, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "witch-billing.ads", name = "banner", matchIfMissing = true, havingValue = "true")
    AdBannerInstaller adBannerInstaller(BillingAds ads) {
        return new AdBannerInstaller(ads);
    }
}
