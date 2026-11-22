package de.witchcafe.billing.server.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.witchcafe.billing.server.ledger.BillingEntry;
import de.witchcafe.billing.server.ledger.EntryKind;
import de.witchcafe.ui.Responsive;

/** Gemeinsame Formatierung und Bausteine der Views. */
final class Format {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE);

    private Format() {}

    static String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant);
    }

    /** Credits mit Vorzeichen, z.B. "+5" oder "-20". */
    static String signed(long credits) {
        return credits > 0 ? "+" + credits : Long.toString(credits);
    }

    static String kind(EntryKind kind) {
        return switch (kind) {
            case WELCOME      -> "Startguthaben";
            case USAGE        -> "Verbrauch";
            case CONTRIBUTION -> "Beitrag";
            case ADMIN_GRANT  -> "Gutschrift";
            case CORRECTION   -> "Korrektur";
        };
    }

    /** Text der Buchung: Feature, sonst Notiz, sonst Art. */
    static String what(BillingEntry e) {
        if (e.getFeature() != null) {
            return e.getQuantity() > 1 ? e.getFeature() + " x" + e.getQuantity() : e.getFeature();
        }
        return e.getNote() != null ? e.getNote() : kind(e.getKind());
    }

    /** Credits als farbiges Badge: gruen fuer Gutschriften, rot fuer Abbuchungen. */
    static Span creditsBadge(long credits) {
        Span badge = new Span(signed(credits));
        badge.getElement().getThemeList().add(credits > 0 ? "badge success" : credits < 0 ? "badge error" : "badge contrast");
        return badge;
    }

    /**
     * Grid fuer Buchungen eines Kontos, auf dem Handy als kompakte Karte pro Zeile.
     */
    static Grid<BillingEntry> entryGrid() {
        Grid<BillingEntry> grid = new Grid<>(BillingEntry.class, false);
        grid.addColumn(e -> dateTime(e.getCreatedAt())).setHeader("Datum").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(BillingEntry::getApp).setHeader("App").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(e -> kind(e.getKind())).setHeader("Art").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Format::what).setHeader("Was").setFlexGrow(1);
        grid.addComponentColumn(e -> creditsBadge(e.getCredits())).setHeader("Credits").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(BillingEntry::getBalanceAfter).setHeader("Stand").setAutoWidth(true).setFlexGrow(0);
        Responsive.mobileCardColumn(grid, new ComponentRenderer<>(e -> {
            Div card = new Div();
            Div top = new Div(new Span(dateTime(e.getCreatedAt()) + " · " + e.getApp() + " "), creditsBadge(e.getCredits()));
            Div bottom = new Div(new Span(what(e) + " · Stand " + e.getBalanceAfter()));
            bottom.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
            card.add(top, bottom);
            return card;
        }));
        return grid;
    }
}
