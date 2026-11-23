package de.witchcafe.billing.server.ledger;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bucht Verbrauch, Beitraege und Admin-Buchungen auf die Guthabenkonten.
 * <p>
 * Jede Buchung sperrt das Konto des Benutzers (Schreibsperre), prueft dann den
 * Idempotenz-Schluessel, berechnet den Betrag aus der Preisliste und schreibt Journal
 * und Kontostand in derselben Transaktion. Damit koennen parallele Anfragen das Konto
 * nicht ins Minus bringen und Wiederholungen nicht doppelt buchen.
 * <p>
 * Mit {@code witch-billing.allow-debt=true} wird Verbrauch auch ohne Guthaben gebucht,
 * das Konto geht dann ins Minus ("Schulden"). Die Apps sperren dann nicht, sondern zeigen
 * Werbung (siehe witch-billing-vaadin).
 * <p>
 * Ein Konto entsteht beim ersten Buchen automatisch, mit dem Startguthaben
 * {@code witch-billing.welcome-credits} als eigener Buchung. Laufen fuer einen ganz neuen
 * Benutzer zwei Anfragen exakt gleichzeitig, scheitert eine davon am Primaerschluessel;
 * der Client wiederholt sie dann mit demselben Idempotenz-Schluessel.
 */
@Service
@Transactional
public class BillingService {

    /** App-Kennung fuer Buchungen, die ein Admin in der Oberflaeche macht. */
    public static final String ADMIN_APP = "admin";

    static final int MAX_IDEMPOTENCY_KEY = 100;

    private final BillingAccountRepository accounts;
    private final BillingEntryRepository entries;
    private final BillingPriceRepository prices;
    private final long welcomeCredits;
    private final boolean allowDebt;

    public BillingService(BillingAccountRepository accounts, BillingEntryRepository entries,
                          BillingPriceRepository prices,
                          @Value("${witch-billing.welcome-credits:100}") long welcomeCredits,
                          @Value("${witch-billing.allow-debt:false}") boolean allowDebt) {
        this.accounts       = accounts;
        this.entries        = entries;
        this.prices         = prices;
        this.welcomeCredits = welcomeCredits;
        this.allowDebt      = allowDebt;
    }

    /** true: Verbrauch wird auch ohne Guthaben gebucht, das Konto geht ins Minus. */
    public boolean isAllowDebt() {
        return allowDebt;
    }

    /**
     * Verbrauch buchen. Kostet {@code quantity} mal den Preis des Features.
     *
     * @throws InsufficientCreditsException wenn das Guthaben nicht reicht und Schulden nicht
     *                                      erlaubt sind (nichts gebucht)
     * @throws UnknownFeatureException      wenn es keinen aktiven COST-Preis gibt
     */
    public Booking charge(String app, String userId, String feature, int quantity, String idempotencyKey) {
        return bookFromPriceList(app, userId, feature, quantity, idempotencyKey, PriceKind.COST);
    }

    /**
     * Beitrag gutschreiben: {@code quantity} mal die Belohnung des Features.
     *
     * @throws UnknownFeatureException wenn es keinen aktiven REWARD-Preis gibt
     */
    public Booking reward(String app, String userId, String feature, int quantity, String idempotencyKey) {
        return bookFromPriceList(app, userId, feature, quantity, idempotencyKey, PriceKind.REWARD);
    }

    /**
     * Manuelle Buchung durch einen Admin, z.B. fuer eine erbrachte Dienstleistung.
     * Positive Betraege sind Gutschriften, negative Korrekturen; eine Korrektur darf
     * das Konto ins Minus bringen.
     */
    public Booking adminBooking(String userId, long credits, String note, String adminUserId) {
        requireText(userId, "userId");
        requireText(note, "note");
        requireText(adminUserId, "adminUserId");
        if (!userId.matches("[^:\\s]+:\\S+")) {
            throw new IllegalArgumentException("userId muss die Form provider:id haben, z.B. github:4711");
        }
        if (credits == 0) {
            throw new IllegalArgumentException("credits darf nicht 0 sein");
        }
        BillingAccount account = lockOrCreate(userId, ADMIN_APP);
        EntryKind kind = credits > 0 ? EntryKind.ADMIN_GRANT : EntryKind.CORRECTION;
        return Booking.of(book(account, ADMIN_APP, kind, null, 1, credits, note, adminUserId, null), false);
    }

    /** Aktueller Kontostand; fuer unbekannte Benutzer das Startguthaben, das sie bekaemen. */
    @Transactional(readOnly = true)
    public long balance(String userId) {
        return accounts.findById(userId).map(BillingAccount::getBalance).orElse(welcomeCredits);
    }

    /** Die letzten Buchungen eines Benutzers, neueste zuerst. */
    @Transactional(readOnly = true)
    public List<BillingEntry> recentEntries(String userId, int limit) {
        return entries.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, limit));
    }

    // --- Admin: Konten, Auswertung, Preisliste -------------------------------------

    /** Konten, deren UserId {@code filter} enthaelt (leer = alle), nach UserId sortiert. */
    @Transactional(readOnly = true)
    public List<BillingAccount> accounts(String filter, int limit) {
        return accounts.findByUserIdContainingIgnoreCaseOrderByUserIdAsc(
                filter == null ? "" : filter.trim(), PageRequest.of(0, limit));
    }

    /** Verbrauch und Beitraege seit {@code since}, pro App, Feature und Art summiert. */
    @Transactional(readOnly = true)
    public List<UsageSummary> usageSince(Instant since) {
        return entries.summarizeUsageSince(since);
    }

    @Transactional(readOnly = true)
    public List<BillingPrice> prices() {
        return prices.findAllByOrderByAppAscFeatureAsc();
    }

    /**
     * Neuer Eintrag in der Preisliste. App und Feature werden klein geschrieben und
     * duerfen nur aus a-z, 0-9, Punkt, Minus und Unterstrich bestehen.
     *
     * @throws IllegalArgumentException bei ungueltigen Namen oder wenn es App/Feature schon gibt
     */
    public BillingPrice createPrice(String app, String feature, PriceKind kind, long creditsPerUnit,
                                    String description) {
        String a = normalizeName(app, "App");
        String f = normalizeName(feature, "Feature");
        if (ADMIN_APP.equals(a)) {
            throw new IllegalArgumentException("\"" + ADMIN_APP + "\" ist fuer Admin-Buchungen reserviert");
        }
        if (prices.findByAppAndFeature(a, f).isPresent()) {
            throw new IllegalArgumentException("Preis fuer " + a + "/" + f + " gibt es schon");
        }
        return prices.save(new BillingPrice(a, f, requireNonNull(kind, "kind"), creditsPerUnit, description));
    }

    /**
     * Preis, Beschreibung und Aktiv-Status aendern. App, Feature und Art bleiben, damit
     * alte Buchungen eindeutig bleiben; Preise werden deaktiviert statt geloescht.
     */
    public BillingPrice updatePrice(long id, long creditsPerUnit, String description, boolean active) {
        BillingPrice price = prices.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Preis " + id + " gibt es nicht"));
        price.setCreditsPerUnit(creditsPerUnit);
        price.setDescription(description);
        price.setActive(active);
        return price;
    }

    private static String normalizeName(String value, String label) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!v.matches("[a-z0-9._-]{1,50}")) {
            throw new IllegalArgumentException(label + " darf nur a-z, 0-9, Punkt, Minus und Unterstrich enthalten (1-50 Zeichen)");
        }
        return v;
    }

    // ---------------------------------------------------------------------------------

    private Booking bookFromPriceList(String app, String userId, String feature, int quantity,
                                      String idempotencyKey, PriceKind expected) {
        requireText(app, "app");
        requireText(userId, "userId");
        requireText(feature, "feature");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity muss > 0 sein");
        }
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY)) {
            throw new IllegalArgumentException("idempotencyKey muss 1.." + MAX_IDEMPOTENCY_KEY + " Zeichen haben");
        }

        BillingPrice price = prices.findByAppAndFeature(app, feature)
                .filter(BillingPrice::isActive)
                .filter(p -> p.getKind() == expected)
                .orElseThrow(() -> new UnknownFeatureException(app, feature, expected));

        BillingAccount account = lockOrCreate(userId, app);

        // Erst nach der Kontosperre pruefen, damit zwei gleiche Anfragen nicht beide durchrutschen
        Optional<BillingEntry> previous = idempotencyKey == null ? Optional.empty()
                : entries.findByAppAndIdempotencyKey(app, idempotencyKey);
        if (previous.isPresent()) {
            return Booking.of(previous.get(), true);
        }

        long amount = Math.multiplyExact(price.getCreditsPerUnit(), (long) quantity);
        long credits;
        EntryKind kind;
        if (expected == PriceKind.COST) {
            if (!allowDebt && account.getBalance() < amount) {
                throw new InsufficientCreditsException(userId, account.getBalance(), amount);
            }
            credits = -amount;
            kind = EntryKind.USAGE;
        } else {
            credits = amount;
            kind = EntryKind.CONTRIBUTION;
        }
        return Booking.of(book(account, app, kind, feature, quantity, credits, null, app, idempotencyKey), false);
    }

    private BillingAccount lockOrCreate(String userId, String app) {
        return accounts.findForUpdate(userId).orElseGet(() -> {
            BillingAccount account = accounts.saveAndFlush(new BillingAccount(userId));
            if (welcomeCredits != 0) {
                book(account, app, EntryKind.WELCOME, null, 1, welcomeCredits, "Startguthaben", "system", null);
            }
            return account;
        });
    }

    private BillingEntry book(BillingAccount account, String app, EntryKind kind, String feature, int quantity,
                              long credits, String note, String bookedBy, String idempotencyKey) {
        account.add(credits);
        BillingEntry entry = new BillingEntry(account.getUserId(), app, kind, feature, quantity, credits,
                account.getBalance(), note, bookedBy, idempotencyKey);
        return entries.save(entry);
    }

    private static void requireText(String value, String name) {
        requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein");
        }
    }
}
