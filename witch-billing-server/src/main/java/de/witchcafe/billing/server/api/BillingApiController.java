package de.witchcafe.billing.server.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.witchcafe.billing.client.api.BalanceResponse;
import de.witchcafe.billing.client.api.BookingRequest;
import de.witchcafe.billing.client.api.BookingResponse;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.billing.server.ledger.Booking;

/**
 * REST-API fuer die Apps. Die App ergibt sich aus dem API-Key ({@link AppKeyFilter}),
 * sie kann also nur Features aus ihrem eigenen Teil der Preisliste buchen.
 * Fehler beantwortet {@link ApiErrorHandler}.
 */
@RestController
@RequestMapping("/api")
public class BillingApiController {

    private final BillingService billing;

    public BillingApiController(BillingService billing) {
        this.billing = billing;
    }

    /** Verbrauch buchen. 402 bei zu wenig Guthaben, 404 bei unbekanntem Feature. */
    @PostMapping("/usage")
    public BookingResponse usage(@RequestBody BookingRequest request, Authentication app) {
        return toResponse(billing.charge(app.getName(), request.userId(), request.feature(),
                request.quantity(), request.idempotencyKey(), request.userLabel(), request.userName()));
    }

    /** Beitrag gutschreiben. 404 bei unbekanntem Feature. */
    @PostMapping("/contributions")
    public BookingResponse contribution(@RequestBody BookingRequest request, Authentication app) {
        return toResponse(billing.reward(app.getName(), request.userId(), request.feature(),
                request.quantity(), request.idempotencyKey(), request.userLabel(), request.userName()));
    }

    /** Kontostand; fuer unbekannte Benutzer das Startguthaben. */
    @GetMapping("/balance/{userId}")
    public BalanceResponse balance(@PathVariable String userId) {
        return new BalanceResponse(userId, billing.balance(userId));
    }

    private static BookingResponse toResponse(Booking b) {
        return new BookingResponse(b.entryId(), b.credits(), b.balanceAfter(), b.replay());
    }
}
