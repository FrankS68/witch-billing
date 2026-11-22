package de.witchcafe.billing.client;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Einstellungen der App fuer den Billing-Dienst (application.properties):
 * <pre>
 * witch-billing.url=http://127.0.0.1:8085
 * witch-billing.api-key=${WITCH_BILLING_API_KEY:}
 * witch-billing.fail-open=true
 * witch-billing.timeout=5s
 * </pre>
 *
 * @param url      Adresse des Dienstes; leer = Billing aus, alles erlaubt
 * @param apiKey   API-Key dieser App (der Dienst erkennt daran die App)
 * @param failOpen true (Default): ist der Dienst nicht erreichbar, wird die Aktion erlaubt
 *                 und eine Warnung geloggt; false: {@link BillingUnavailableException}
 * @param timeout  Verbindungs- und Lese-Timeout pro Aufruf (Default 5s)
 */
@ConfigurationProperties("witch-billing")
public record BillingProperties(String url, String apiKey, Boolean failOpen, Duration timeout) {

    public boolean enabled() {
        return url != null && !url.isBlank();
    }

    public boolean isFailOpen() {
        return failOpen == null || failOpen;
    }

    public Duration timeoutOrDefault() {
        return timeout == null ? Duration.ofSeconds(5) : timeout;
    }
}
