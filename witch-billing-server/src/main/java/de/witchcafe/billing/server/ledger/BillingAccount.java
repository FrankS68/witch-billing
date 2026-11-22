package de.witchcafe.billing.server.ledger;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Guthabenkonto eines Benutzers, gemeinsam fuer alle Apps.
 * <p>
 * Schluessel ist die witch-auth-UserId ("provider:providerId"). Der Kontostand ist ein
 * Zwischenspeicher der Summe aller {@link BillingEntry Buchungen} und wird nur von
 * {@link BillingService} veraendert, immer zusammen mit einer neuen Buchung.
 */
@Entity
@Table(name = "billing_account")
public class BillingAccount {

    @Id
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(nullable = false)
    private long balance;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;

    protected BillingAccount() {}

    BillingAccount(String userId) {
        this.userId = userId;
    }

    public String getUserId()     { return userId; }
    public long getBalance()      { return balance; }
    public Instant getCreatedAt() { return createdAt; }

    void add(long credits) {
        this.balance = Math.addExact(this.balance, credits);
    }
}
