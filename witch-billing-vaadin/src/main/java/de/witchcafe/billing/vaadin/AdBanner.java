package de.witchcafe.billing.vaadin;

import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

import de.witchcafe.billing.client.api.AdsResponse;

/**
 * Werbe-Banner unten auf jeder Seite, nur fuer Benutzer im Minus. Laesst sich wegklicken,
 * erscheint aber nach dem naechsten Seitenwechsel wieder. Ohne gepflegte Werbung steht dort
 * ein Hinweis, wie man das Guthaben wieder auffuellt.
 */
public class AdBanner extends Div {

    private final BillingAds ads;
    private final Div content = new Div();

    public AdBanner(BillingAds ads) {
        this.ads = ads;
        addClassName("witch-ad-banner");
        getStyle().set("position", "fixed").set("left", "0").set("right", "0").set("bottom", "0")
                .set("z-index", "100")
                .set("display", "flex").set("align-items", "center").set("gap", "var(--lumo-space-s)")
                .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("background", "var(--lumo-base-color)")
                .set("border-top", "1px solid var(--lumo-contrast-20pct)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");
        content.getStyle().set("flex", "1").set("min-width", "0");

        Button close = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> setVisible(false));
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        close.setAriaLabel("Werbung ausblenden");

        add(content, close);
        setVisible(false);
    }

    /** Neu befuellen (nach jedem Seitenwechsel). */
    public void refresh() {
        update(ads.current());
    }

    void update(Optional<AdsResponse> response) {
        content.removeAll();
        if (response.isEmpty() || !response.get().inDebt()) {
            setVisible(false);
            return;
        }
        AdsResponse r = response.get();
        content.add(ads.pick(r)
                .map(ad -> AdCard.of(ad, ads.properties()))
                .orElseGet(() -> fallback(r.balance(), ads.creditsUrl())));
        setVisible(true);
    }

    /** Platzhalter, solange keine Werbung gepflegt ist. */
    static Div fallback(long balance, String creditsUrl) {
        Div div = new Div(new Span("Dein Guthaben ist im Minus (" + balance + " Credits). "
                + "Mit Beiträgen in den Apps füllst du es wieder auf."));
        if (creditsUrl != null && !creditsUrl.isBlank()) {
            Anchor link = new Anchor(creditsUrl, "Guthaben ansehen");
            link.getElement().setAttribute("target", "_blank");
            link.getStyle().set("margin-left", "var(--lumo-space-s)");
            div.add(link);
        }
        return div;
    }
}
