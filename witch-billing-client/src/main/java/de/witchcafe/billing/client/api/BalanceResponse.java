package de.witchcafe.billing.client.api;

/** Antwort auf {@code GET /api/balance/{userId}}. */
public record BalanceResponse(String userId, long balance) {
}
