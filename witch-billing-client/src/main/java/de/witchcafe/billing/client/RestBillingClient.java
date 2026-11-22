package de.witchcafe.billing.client;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import de.witchcafe.billing.client.api.BalanceResponse;
import de.witchcafe.billing.client.api.BookingRequest;
import de.witchcafe.billing.client.api.BookingResponse;
import de.witchcafe.billing.client.api.ErrorResponse;

/**
 * {@link BillingClient} ueber die REST-API des Billing-Dienstes.
 * <p>
 * Der {@link RestClient} muss Basis-URL und "Authorization: Bearer &lt;API-Key&gt;" schon
 * mitbringen (macht {@link BillingAutoConfiguration}).
 * <ul>
 * <li>Netzwerkfehler werden einmal wiederholt, Buchungen nur mit Idempotenz-Schluessel
 *     (sonst koennte doppelt gebucht werden).</li>
 * <li>Nicht erreichbar, 5xx oder abgelehnter API-Key: bei fail-open wird
 *     {@link BookingResponse#NOT_BOOKED} geliefert und gewarnt, sonst
 *     {@link BillingUnavailableException}. {@link #balance(String)} liefert dann leer.</li>
 * </ul>
 */
public class RestBillingClient implements BillingClient {

    private static final Logger log = LoggerFactory.getLogger(RestBillingClient.class);

    private final RestClient rest;
    private final boolean failOpen;
    private final BillingUserResolver userResolver;

    public RestBillingClient(RestClient rest, boolean failOpen, BillingUserResolver userResolver) {
        this.rest         = rest;
        this.failOpen     = failOpen;
        this.userResolver = userResolver;
    }

    @Override
    public BookingResponse charge(String userId, String feature, int quantity, String idempotencyKey) {
        return book("/api/usage", new BookingRequest(userId, feature, quantity, idempotencyKey));
    }

    @Override
    public BookingResponse reward(String userId, String feature, int quantity, String idempotencyKey) {
        return book("/api/contributions", new BookingRequest(userId, feature, quantity, idempotencyKey));
    }

    @Override
    public OptionalLong balance(String userId) {
        try {
            BalanceResponse response = call(true, () -> rest.get()
                    .uri("/api/balance/{userId}", userId)
                    .retrieve()
                    .body(BalanceResponse.class));
            return response == null ? OptionalLong.empty() : OptionalLong.of(response.balance());
        } catch (BillingUnavailableException e) {
            log.warn("Kontostand von {} nicht abrufbar: {}", userId, e.getMessage());
            return OptionalLong.empty();
        }
    }

    @Override
    public Optional<String> currentUserId() {
        return userResolver.currentUserId();
    }

    // ---------------------------------------------------------------------------------

    private BookingResponse book(String path, BookingRequest request) {
        try {
            BookingResponse response = call(request.idempotencyKey() != null, () -> rest.post()
                    .uri(path)
                    .body(request)
                    .retrieve()
                    .body(BookingResponse.class));
            if (response == null) {
                throw new BillingUnavailableException("Leere Antwort von " + path, null);
            }
            return response;
        } catch (BillingUnavailableException e) {
            if (!failOpen) {
                throw e;
            }
            log.warn("Billing nicht verfuegbar, {} {}x{} fuer {} wird ohne Buchung erlaubt: {}",
                    path, request.feature(), request.quantity(), request.userId(), e.getMessage());
            return BookingResponse.NOT_BOOKED;
        }
    }

    /** Fuehrt den Aufruf aus, wiederholt ihn ggf. einmal bei Netzwerkfehler und uebersetzt Fehler. */
    private <T> T call(boolean retryAllowed, Supplier<T> request) {
        try {
            try {
                return request.get();
            } catch (ResourceAccessException e) {
                if (!retryAllowed) {
                    throw e;
                }
                log.debug("Billing-Aufruf fehlgeschlagen, zweiter Versuch: {}", e.getMessage());
                return request.get();
            }
        } catch (HttpClientErrorException e) {
            throw translate(e);
        } catch (RestClientResponseException e) {
            throw new BillingUnavailableException("HTTP " + e.getStatusCode().value() + " vom Billing-Dienst", e);
        } catch (RestClientException e) {
            throw new BillingUnavailableException(e.getMessage(), e);
        }
    }

    private static BillingException translate(HttpClientErrorException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            log.error("Billing-Dienst lehnt den API-Key ab (witch-billing.api-key pruefen)");
            return new BillingUnavailableException("API-Key abgelehnt (HTTP " + e.getStatusCode().value() + ")", e);
        }
        ErrorResponse error = errorBody(e);
        String message = error != null && error.message() != null ? error.message() : e.getMessage();
        if (status == HttpStatus.PAYMENT_REQUIRED) {
            long balance  = error != null && error.balance()  != null ? error.balance()  : 0;
            long required = error != null && error.required() != null ? error.required() : 0;
            return new InsufficientCreditException(message, balance, required);
        }
        String code = error != null && error.code() != null ? error.code() : "HTTP_" + e.getStatusCode().value();
        return new BillingException(code, message, e);
    }

    private static ErrorResponse errorBody(RestClientResponseException e) {
        try {
            return e.getResponseBodyAs(ErrorResponse.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
