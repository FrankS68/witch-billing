package de.witchcafe.billing.server.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Prueft "Authorization: Bearer &lt;API-Key&gt;" und meldet die App an, zu der der Key gehoert.
 * Name der Authentifizierung ist die App-Kennung, Rolle {@value #ROLE}.
 * <p>
 * Bewusst keine @Component: sonst registriert Spring Boot den Filter zusaetzlich global
 * fuer alle Anfragen. Er wird nur in {@link ApiSecurityConfig} in die /api-Kette gehaengt.
 */
class AppKeyFilter extends OncePerRequestFilter {

    static final String ROLE = "ROLE_BILLING_APP";

    private static final Logger log = LoggerFactory.getLogger(AppKeyFilter.class);

    /** App-Kennung -&gt; SHA-256-Hash des Keys */
    private final Map<String, byte[]> hashesByApp;

    AppKeyFilter(AppKeyProperties properties) {
        HexFormat hex = HexFormat.of();
        this.hashesByApp = properties.appKeys().entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, e -> hex.parseHex(e.getValue().trim().toLowerCase())));
        if (hashesByApp.isEmpty()) {
            log.warn("Keine API-Keys konfiguriert (witch-billing.app-keys.*) - die REST-API lehnt alle Anfragen ab");
        } else {
            log.info("API-Keys konfiguriert fuer: {}", hashesByApp.keySet());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            appFor(header.substring(7).trim()).ifPresent(app -> {
                var auth = new UsernamePasswordAuthenticationToken(app, null,
                        List.of(new SimpleGrantedAuthority(ROLE)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }

    Optional<String> appFor(String key) {
        if (key.isEmpty()) {
            return Optional.empty();
        }
        byte[] hash = sha256(key);
        // Alle Eintraege vergleichen, zeitkonstant pro Vergleich
        String found = null;
        for (var e : hashesByApp.entrySet()) {
            if (MessageDigest.isEqual(e.getValue(), hash)) {
                found = e.getKey();
            }
        }
        return Optional.ofNullable(found);
    }

    static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
