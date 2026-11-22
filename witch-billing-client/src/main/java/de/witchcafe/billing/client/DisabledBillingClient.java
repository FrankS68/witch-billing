package de.witchcafe.billing.client;

import java.util.Optional;
import java.util.OptionalLong;

import de.witchcafe.billing.client.api.BookingResponse;

/**
 * {@link BillingClient} ohne Dienst ({@code witch-billing.url} leer): bucht nichts und
 * erlaubt alles. So kann eine App den Client immer einbinden, auch lokal ohne Billing.
 */
public class DisabledBillingClient implements BillingClient {

    private final BillingUserResolver userResolver;

    public DisabledBillingClient(BillingUserResolver userResolver) {
        this.userResolver = userResolver;
    }

    @Override
    public BookingResponse charge(String userId, String feature, int quantity, String idempotencyKey) {
        return BookingResponse.NOT_BOOKED;
    }

    @Override
    public BookingResponse reward(String userId, String feature, int quantity, String idempotencyKey) {
        return BookingResponse.NOT_BOOKED;
    }

    @Override
    public OptionalLong balance(String userId) {
        return OptionalLong.empty();
    }

    @Override
    public Optional<String> currentUserId() {
        return userResolver.currentUserId();
    }
}
