package de.witchcafe.billing.vaadin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Haengt in jede Vaadin-Seite der App ein {@link AdBanner} und aktualisiert es nach jedem
 * Seitenwechsel. So braucht weder witch-ui noch die App ihr Layout zu aendern.
 */
public class AdBannerInstaller implements VaadinServiceInitListener {

    private final BillingAds ads;

    public AdBannerInstaller(BillingAds ads) {
        this.ads = ads;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(e -> install(e.getUI()));
    }

    void install(UI ui) {
        AdBanner banner = new AdBanner(ads);
        ui.addAfterNavigationListener(nav -> {
            if (banner.getParent().isEmpty()) {
                ui.add(banner);
            }
            banner.refresh();
        });
    }
}
