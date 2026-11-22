package de.witchcafe.billing.server.api;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Eigene Filterkette fuer die REST-API unter /api/**: zustandslos, ohne CSRF,
 * Anmeldung nur per API-Key einer App. Die Vaadin-Oberflaeche sichert weiter witch-auth ab;
 * die Kette hat deshalb einen securityMatcher, einen eigenen Bean-Namen und Vorrang.
 */
@Configuration
@EnableConfigurationProperties(AppKeyProperties.class)
public class ApiSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain billingApiFilterChain(HttpSecurity http, AppKeyProperties appKeys) throws Exception {
        http.securityMatcher("/api/**")
                .addFilterBefore(new AppKeyFilter(appKeys), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(AppKeyFilter.ROLE))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
