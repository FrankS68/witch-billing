package de.witchcafe.billing.server.ads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Eine Werbung, die der Billing-Dienst an die Apps ausliefert. Gepflegt unter
 * {@code /admin/werbung}; nicht mehr benoetigte Eintraege werden deaktiviert.
 */
@Entity
@Table(name = "billing_ad")
public class BillingAd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AdKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AdAudience audience;

    /** App, in der die Werbung erscheint (quiz, games, garten); leer = in allen Apps. */
    @Column(length = 50)
    private String app;

    @Column(length = 100)
    private String title;

    // "text" ist in manchen Datenbanken ein Typname, daher eigener Spaltenname
    @Column(name = "ad_text", length = 500)
    private String text;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 1000)
    private String linkUrl;

    @Column(length = 50)
    private String adsenseSlot;

    @Column(nullable = false)
    private boolean active = true;

    protected BillingAd() {}

    public BillingAd(AdKind kind, AdAudience audience) {
        this.kind     = kind;
        this.audience = audience;
    }

    public Long getId()             { return id; }
    public AdKind getKind()         { return kind; }
    public AdAudience getAudience() { return audience; }
    public String getApp()          { return app; }
    public String getTitle()        { return title; }
    public String getText()         { return text; }
    public String getImageUrl()     { return imageUrl; }
    public String getLinkUrl()      { return linkUrl; }
    public String getAdsenseSlot()  { return adsenseSlot; }
    public boolean isActive()       { return active; }

    public void setKind(AdKind kind)             { this.kind = kind; }
    public void setAudience(AdAudience audience) { this.audience = audience; }
    public void setApp(String app)               { this.app = app; }
    public void setTitle(String title)           { this.title = title; }
    public void setText(String text)             { this.text = text; }
    public void setImageUrl(String imageUrl)     { this.imageUrl = imageUrl; }
    public void setLinkUrl(String linkUrl)       { this.linkUrl = linkUrl; }
    public void setAdsenseSlot(String slot)      { this.adsenseSlot = slot; }
    public void setActive(boolean active)        { this.active = active; }

    /** Gilt fuer diese App (leeres {@link #app} = alle Apps). */
    public boolean appliesTo(String appName) {
        return app == null || app.isBlank() || app.equalsIgnoreCase(appName);
    }
}
