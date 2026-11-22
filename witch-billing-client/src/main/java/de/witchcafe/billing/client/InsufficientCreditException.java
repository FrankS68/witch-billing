package de.witchcafe.billing.client;

import de.witchcafe.billing.client.api.ErrorResponse;

/**
 * Das Guthaben des Benutzers reicht nicht fuer die gewuenschte Funktion (HTTP 402).
 * Es wurde nichts gebucht; die App darf die Funktion nicht ausfuehren.
 */
public class InsufficientCreditException extends BillingException {

    private final long balance;
    private final long required;

    public InsufficientCreditException(String message, long balance, long required) {
        super(ErrorResponse.INSUFFICIENT_CREDITS, message);
        this.balance  = balance;
        this.required = required;
    }

    public long getBalance()  { return balance; }
    public long getRequired() { return required; }
}
