package de.witchcafe.billing.server.ledger;

/**
 * Das Guthaben reicht fuer den Verbrauch nicht aus. Es wurde nichts gebucht.
 */
public class InsufficientCreditsException extends RuntimeException {

    private final long balance;
    private final long required;

    public InsufficientCreditsException(String userId, long balance, long required) {
        super("Guthaben von " + userId + " reicht nicht: " + balance + " vorhanden, " + required + " noetig");
        this.balance  = balance;
        this.required = required;
    }

    public long getBalance()  { return balance; }
    public long getRequired() { return required; }
}
