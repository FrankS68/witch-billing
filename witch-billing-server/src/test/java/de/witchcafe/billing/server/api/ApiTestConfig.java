package de.witchcafe.billing.server.api;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Eigene Konfiguration fuer die API-Tests, damit @WebMvcTest nicht die
 * BillingServerApplication mit ihrem @ComponentScan("de.witchcafe") (Vaadin, witch-auth) nimmt.
 */
@SpringBootConfiguration
@EnableWebSecurity
@Import({ApiSecurityConfig.class, BillingApiController.class, AdsApiController.class, ApiErrorHandler.class})
class ApiTestConfig {
}
