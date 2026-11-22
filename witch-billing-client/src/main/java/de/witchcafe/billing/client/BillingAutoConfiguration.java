package de.witchcafe.billing.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestClient;

/**
 * Stellt einen {@link BillingClient} bereit, sobald witch-billing-client auf dem Klassenpfad ist:
 * mit {@code witch-billing.url} einen {@link RestBillingClient}, sonst einen
 * {@link DisabledBillingClient}, der alles erlaubt. Eigene Beans gleichen Typs haben Vorrang.
 */
@AutoConfiguration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BillingAutoConfiguration.class);

    private static final String CURRENT_USER_CLASS = "de.witchcafe.auth.CurrentUser";

    @Bean
    @ConditionalOnMissingBean
    BillingUserResolver billingUserResolver(ApplicationContext context) {
        if (ClassUtils.isPresent(CURRENT_USER_CLASS, getClass().getClassLoader())) {
            return WitchAuthResolverFactory.create(context);
        }
        return Optional::empty;
    }

    @Bean
    @ConditionalOnMissingBean
    BillingClient billingClient(BillingProperties properties, BillingUserResolver userResolver,
                                ObjectProvider<RestClient.Builder> builders) {
        if (!properties.enabled()) {
            log.info("witch-billing.url nicht gesetzt - Billing ausgeschaltet, alle Funktionen frei");
            return new DisabledBillingClient(userResolver);
        }
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.warn("witch-billing.api-key nicht gesetzt - der Billing-Dienst wird alle Buchungen ablehnen");
        }
        Duration timeout = properties.timeoutOrDefault();
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        requestFactory.setReadTimeout(timeout);

        RestClient rest = builders.getIfAvailable(RestClient::builder).clone()
                .baseUrl(properties.url())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .requestFactory(requestFactory)
                .build();
        log.info("Billing ueber {} (fail-open={})", properties.url(), properties.isFailOpen());
        return new RestBillingClient(rest, properties.isFailOpen(), userResolver);
    }

    /** Eigene Klasse, damit CurrentUser nur angefasst wird, wenn witch-auth vorhanden ist. */
    private static final class WitchAuthResolverFactory {
        static BillingUserResolver create(ApplicationContext context) {
            return new WitchAuthUserResolver(context.getBeanProvider(de.witchcafe.auth.CurrentUser.class));
        }
    }
}
