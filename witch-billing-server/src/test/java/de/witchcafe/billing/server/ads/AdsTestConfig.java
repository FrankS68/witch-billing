package de.witchcafe.billing.server.ads;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

/**
 * Eigene Konfiguration fuer die Werbe-Tests (wie LedgerTestConfig): JPA findet Entities und
 * Repositories aus ads und ledger, ohne dass Vaadin und witch-auth mitgeladen werden.
 */
@SpringBootConfiguration
@AutoConfigurationPackage(basePackages = "de.witchcafe.billing.server")
class AdsTestConfig {
}
