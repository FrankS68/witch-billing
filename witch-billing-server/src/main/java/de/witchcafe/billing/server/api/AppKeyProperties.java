package de.witchcafe.billing.server.api;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API-Keys der Apps, als SHA-256-Hash (hex, klein) pro App-Kennung:
 * <pre>
 * witch-billing.app-keys.quiz=${BILLING_KEY_HASH_QUIZ:}
 * witch-billing.app-keys.games=${BILLING_KEY_HASH_GAMES:}
 * </pre>
 * Die App-Kennung ist zugleich der App-Name in der Preisliste und im Journal.
 * Im Klartext kennt den Key nur die jeweilige App (witch-billing.api-key).
 */
@ConfigurationProperties("witch-billing")
public record AppKeyProperties(Map<String, String> appKeys) {

    public AppKeyProperties {
        appKeys = appKeys == null ? Map.of() : Map.copyOf(appKeys);
    }
}
