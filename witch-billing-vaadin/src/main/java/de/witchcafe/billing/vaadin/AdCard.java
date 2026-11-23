package de.witchcafe.billing.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;

import de.witchcafe.billing.client.api.AdResponse;

/**
 * Darstellung einer Werbung: eigene Werbung und Amazon-Links als kleine Karte mit
 * "Anzeige"-Kennzeichnung (Amazon zusaetzlich mit Pflicht-Hinweis), AdSense als Anzeigenblock.
 */
final class AdCard {

    static final String AMAZON_HINT = "Als Amazon-Partner verdiene ich an qualifizierten Verkäufen.";

    private static final String ADSENSE_LOADED = "witch-billing-adsense-loaded";

    private AdCard() {}

    static Component of(AdResponse ad, AdsProperties properties) {
        if (AdResponse.ADSENSE.equals(ad.kind())) {
            return adsense(ad, properties.adsenseClient());
        }
        Div card = new Div();
        card.addClassName("witch-ad");
        card.getStyle().set("display", "flex").set("gap", "var(--lumo-space-m)")
                .set("align-items", "center").set("min-width", "0");

        if (ad.imageUrl() != null && !ad.imageUrl().isBlank()) {
            Image image = new Image(ad.imageUrl(), "");
            image.getStyle().set("max-height", "3em").set("max-width", "5em").set("object-fit", "contain");
            card.add(image);
        }

        Div texts = new Div();
        texts.getStyle().set("min-width", "0");
        Span label = new Span("Anzeige");
        label.getStyle().set("font-size", "var(--lumo-font-size-xxs)")
                .set("color", "var(--lumo-secondary-text-color)").set("display", "block");
        texts.add(label);
        if (ad.title() != null && !ad.title().isBlank()) {
            Span title = new Span(ad.title());
            title.getStyle().set("font-weight", "600").set("display", "block");
            texts.add(title);
        }
        if (ad.text() != null && !ad.text().isBlank()) {
            Span text = new Span(ad.text());
            text.getStyle().set("font-size", "var(--lumo-font-size-s)").set("display", "block");
            texts.add(text);
        }
        if (AdResponse.AMAZON.equals(ad.kind())) {
            Span hint = new Span(AMAZON_HINT);
            hint.getStyle().set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-secondary-text-color)").set("display", "block");
            texts.add(hint);
        }
        card.add(texts);

        if (ad.linkUrl() == null || ad.linkUrl().isBlank()) {
            return card;
        }
        Anchor link = new Anchor(ad.linkUrl(), card);
        link.setTarget(AnchorTarget.BLANK);
        link.getElement().setAttribute("rel", "sponsored noopener noreferrer");
        link.getStyle().set("color", "inherit").set("text-decoration", "none").set("min-width", "0");
        return link;
    }

    /**
     * AdSense-Anzeigenblock. Das Google-Skript wird einmal pro Seite geladen; die Einwilligung
     * (Datenschutz-Meldung) richtet man im AdSense-Konto ein, Google blendet sie dann selbst ein.
     */
    private static Component adsense(AdResponse ad, String client) {
        Div box = new Div();
        box.getStyle().set("min-width", "250px").set("width", "100%");
        Element ins = new Element("ins");
        ins.setAttribute("class", "adsbygoogle");
        ins.getStyle().set("display", "block");
        ins.setAttribute("data-ad-client", client);
        ins.setAttribute("data-ad-slot", ad.adsenseSlot());
        ins.setAttribute("data-ad-format", "auto");
        ins.setAttribute("data-full-width-responsive", "true");
        box.getElement().appendChild(ins);

        UI ui = UI.getCurrent();
        if (ui != null) {
            if (ui.getSession().getAttribute(ADSENSE_LOADED) == null) {
                ui.getSession().setAttribute(ADSENSE_LOADED, Boolean.TRUE);
                ui.getPage().executeJs("""
                        if (!document.querySelector('script[data-witch-adsense]')) {
                          const s = document.createElement('script');
                          s.async = true; s.crossOrigin = 'anonymous'; s.dataset.witchAdsense = '1';
                          s.src = 'https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=' + encodeURIComponent($0);
                          document.head.appendChild(s);
                        }""", client);
            }
            box.addAttachListener(e -> ins.executeJs("(window.adsbygoogle = window.adsbygoogle || []).push({});"));
        }
        return box;
    }
}
