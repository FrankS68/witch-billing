package de.witchcafe.billing.server.ledger;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

/**
 * Eigene Konfiguration fuer die Ledger-Tests. Ohne sie wuerde @DataJpaTest die
 * BillingServerApplication nehmen, deren @ComponentScan("de.witchcafe") Vaadin und
 * witch-auth mitladen wuerde. @AutoConfigurationPackage laesst JPA die Entities und
 * Repositories in diesem Paket finden; die JPA-Auto-Configuration bringt @DataJpaTest mit.
 */
@SpringBootConfiguration
@AutoConfigurationPackage
class LedgerTestConfig {
}
