package de.witchcafe.billing.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;

/**
 * Kurze Werbe-Einblendung, wenn eine Buchung das Konto ins Minus bringt oder es dort laesst:
 * ein Dialog, der sich erst nach einigen Sekunden schliessen laesst. Die Aktion selbst ist dann
 * schon erlaubt und gestartet; die Einblendung liegt nur davor. Der Countdown laeuft im
 * Browser, Server-Push ist dafuer nicht noetig.
 */
public final class AdBreak {

    private AdBreak() {}

    static void show(BillingAds ads, long balance) {
        int seconds = ads.properties().adBreakSecondsOrDefault();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Werbepause");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setMaxWidth("36em");

        dialog.add(new Paragraph("Dein Guthaben ist im Minus (" + balance + " Credits). "
                + "Statt dich zu sperren, zeigen wir dir kurz Werbung."));
        dialog.add(ads.current()
                .flatMap(ads::pick)
                .map(ad -> AdCard.of(ad, ads.properties()))
                .orElseGet(() -> AdBanner.fallback(balance, ads.creditsUrl())));

        Button next = new Button(seconds > 0 ? "Weiter (" + seconds + ")" : "Weiter", e -> dialog.close());
        next.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(next);
        if (seconds > 0) {
            next.getElement().executeJs("""
                    const b = this; let n = $0;
                    b.disabled = true;
                    const t = setInterval(() => {
                      n--;
                      if (n <= 0) { clearInterval(t); b.disabled = false; b.textContent = 'Weiter'; }
                      else { b.textContent = 'Weiter (' + n + ')'; }
                    }, 1000);""", seconds);
        }
        dialog.open();
    }
}
