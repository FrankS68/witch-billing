package de.witchcafe.billing.server.ledger;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Eine Buchung im Journal. Buchungen werden nur angelegt, nie geaendert oder geloescht;
 * Fehler werden mit einer {@link EntryKind#CORRECTION Korrekturbuchung} ausgeglichen.
 * <p>
 * Der Idempotenz-Schluessel ist pro App eindeutig: schickt eine App dieselbe Anfrage
 * nach einem Netzwerkfehler noch einmal, wird nicht doppelt gebucht.
 */
@Entity
@Table(name = "billing_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_billing_entry_idempotency",
                columnNames = {"app", "idempotency_key"}),
        indexes = @Index(name = "ix_billing_entry_user", columnList = "user_id, created_at"))
public class BillingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /** App, die gebucht hat ("quiz", "games", "garten"), bei Admin-Buchungen "admin". */
    @Column(nullable = false, length = 50)
    private String app;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryKind kind;

    /** Feature laut Preisliste, z.B. "ki-fragen"; bei Admin-Buchungen leer. */
    @Column(length = 100)
    private String feature;

    @Column(nullable = false)
    private int quantity;

    /** Betrag mit Vorzeichen: positiv = Gutschrift, negativ = Verbrauch. */
    @Column(nullable = false)
    private long credits;

    /** Kontostand direkt nach dieser Buchung. */
    @Column(nullable = false)
    private long balanceAfter;

    @Column(length = 500)
    private String note;

    /** Wer gebucht hat: App-Name oder UserId des Admins. */
    @Column(nullable = false, length = 255)
    private String bookedBy;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected BillingEntry() {}

    BillingEntry(String userId, String app, EntryKind kind, String feature, int quantity,
                 long credits, long balanceAfter, String note, String bookedBy, String idempotencyKey) {
        this.userId         = userId;
        this.app            = app;
        this.kind           = kind;
        this.feature        = feature;
        this.quantity       = quantity;
        this.credits        = credits;
        this.balanceAfter   = balanceAfter;
        this.note           = note;
        this.bookedBy       = bookedBy;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId()                { return id; }
    public String getUserId()          { return userId; }
    public String getApp()             { return app; }
    public EntryKind getKind()         { return kind; }
    public String getFeature()         { return feature; }
    public int getQuantity()           { return quantity; }
    public long getCredits()           { return credits; }
    public long getBalanceAfter()      { return balanceAfter; }
    public String getNote()            { return note; }
    public String getBookedBy()        { return bookedBy; }
    public String getIdempotencyKey()  { return idempotencyKey; }
    public Instant getCreatedAt()      { return createdAt; }
}
