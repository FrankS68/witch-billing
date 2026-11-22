package de.witchcafe.billing.server.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Eintrag der Preisliste: was ein Feature einer App pro Einheit kostet oder einbringt.
 * <p>
 * Die Apps schicken nur Feature und Menge, nie einen Betrag. Ein Preis von 0 ist erlaubt:
 * dann wird die Nutzung nur gezaehlt.
 */
@Entity
@Table(name = "billing_price",
        uniqueConstraints = @UniqueConstraint(name = "uk_billing_price_feature",
                columnNames = {"app", "feature"}))
public class BillingPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String app;

    @Column(nullable = false, length = 100)
    private String feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PriceKind kind;

    /** Credits pro Einheit, immer &gt;= 0; das Vorzeichen ergibt sich aus {@link #kind}. */
    @Column(nullable = false)
    private long creditsPerUnit;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected BillingPrice() {}

    public BillingPrice(String app, String feature, PriceKind kind, long creditsPerUnit, String description) {
        if (creditsPerUnit < 0) {
            throw new IllegalArgumentException("creditsPerUnit muss >= 0 sein");
        }
        this.app            = app;
        this.feature        = feature;
        this.kind           = kind;
        this.creditsPerUnit = creditsPerUnit;
        this.description    = description;
    }

    public Long getId()             { return id; }
    public String getApp()          { return app; }
    public String getFeature()      { return feature; }
    public PriceKind getKind()      { return kind; }
    public long getCreditsPerUnit() { return creditsPerUnit; }
    public String getDescription()  { return description; }
    public boolean isActive()       { return active; }

    public void setCreditsPerUnit(long creditsPerUnit) {
        if (creditsPerUnit < 0) {
            throw new IllegalArgumentException("creditsPerUnit muss >= 0 sein");
        }
        this.creditsPerUnit = creditsPerUnit;
    }

    public void setDescription(String description) { this.description = description; }
    public void setActive(boolean active)          { this.active = active; }
}
