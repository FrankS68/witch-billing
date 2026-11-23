package de.witchcafe.billing.server.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.billing.server.ads.AdAudience;
import de.witchcafe.billing.server.ads.AdKind;
import de.witchcafe.billing.server.ads.AdService;
import de.witchcafe.billing.server.ads.BillingAd;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.RolesAllowed;

/**
 * Admin: Werbung pflegen, die die Apps ueber witch-billing-vaadin anzeigen. Benutzer im Minus
 * sehen sie statt einer Sperre als Banner und kurze Einblendung beim Start einer Partie.
 * Eintraege werden deaktiviert statt geloescht.
 */
@Route("admin/werbung")
@PageTitle("Werbung")
@Menu(order = 92, icon = "vaadin:megaphone", title = "Werbung")
@RolesAllowed(AppRoles.ADMIN)
public class AdsAdminView extends VerticalLayout {

    private final AdService ads;
    private final Grid<BillingAd> grid = new Grid<>(BillingAd.class, false);

    public AdsAdminView(AdService ads, BillingService billing) {
        this.ads = ads;
        setSizeFull();

        Button create = new Button("Neue Werbung", VaadinIcon.PLUS.create(), e -> openDialog(null));
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Paragraph hint = new Paragraph(billing.isAllowDebt()
                ? "Konten dürfen ins Minus gehen (witch-billing.allow-debt=true): wer im Minus ist, wird nicht gesperrt, sondern sieht Werbung."
                : "Achtung: witch-billing.allow-debt=false – wer kein Guthaben hat, wird gesperrt; Werbung sehen nur Einträge \"Alle Benutzer\".");
        hint.addClassName("text-secondary");

        grid.addColumn(ad -> kindLabel(ad.getKind())).setHeader("Art").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(ad -> ad.getApp() == null ? "alle" : ad.getApp()).setHeader("App").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(ad -> audienceLabel(ad.getAudience())).setHeader("Zielgruppe").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(AdsAdminView::summary).setHeader("Inhalt").setFlexGrow(1);
        var linkColumn = grid.addColumn(ad -> ad.getLinkUrl() == null ? "" : ad.getLinkUrl())
                .setHeader("Link").setFlexGrow(1);
        grid.addComponentColumn(ad -> {
            Span badge = new Span(ad.isActive() ? "Aktiv" : "Aus");
            badge.getElement().getThemeList().add("badge");
            badge.getElement().getThemeList().add(ad.isActive() ? "success" : "contrast");
            return badge;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        grid.addItemClickListener(e -> openDialog(e.getItem()));
        Responsive.hideOnMobile(grid, linkColumn);
        grid.setSizeFull();

        add(new ViewToolbar("Werbung", create), hint, grid);
        refresh();
    }

    private void refresh() {
        grid.setItems(ads.all());
    }

    /** Dialog zum Anlegen ({@code existing == null}) oder Bearbeiten. */
    private void openDialog(BillingAd existing) {
        boolean isNew = existing == null;
        BillingAd ad = isNew ? new BillingAd(AdKind.HOUSE, AdAudience.DEBTORS) : existing;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Neue Werbung" : "Werbung bearbeiten");

        Select<AdKind> kindField = new Select<>();
        kindField.setLabel("Art");
        kindField.setItems(AdKind.values());
        kindField.setItemLabelGenerator(AdsAdminView::kindLabel);
        Select<AdAudience> audienceField = new Select<>();
        audienceField.setLabel("Zielgruppe");
        audienceField.setItems(AdAudience.values());
        audienceField.setItemLabelGenerator(AdsAdminView::audienceLabel);
        TextField appField = new TextField("App");
        appField.setPlaceholder("leer = alle Apps");
        appField.setHelperText("quiz, games, garten");
        TextField titleField = new TextField("Titel");
        titleField.setMaxLength(100);
        TextArea textField = new TextArea("Text");
        textField.setMaxLength(500);
        TextField linkField = new TextField("Link");
        linkField.setMaxLength(1000);
        linkField.setPlaceholder("https://…");
        TextField imageField = new TextField("Bild-URL");
        imageField.setMaxLength(500);
        imageField.setPlaceholder("https://… (optional)");
        TextField slotField = new TextField("AdSense-Anzeigenblock");
        slotField.setPlaceholder("nur Ziffern");
        Checkbox activeField = new Checkbox("Aktiv");

        kindField.setValue(ad.getKind());
        audienceField.setValue(ad.getAudience());
        appField.setValue(nonNull(ad.getApp()));
        titleField.setValue(nonNull(ad.getTitle()));
        textField.setValue(nonNull(ad.getText()));
        linkField.setValue(nonNull(ad.getLinkUrl()));
        imageField.setValue(nonNull(ad.getImageUrl()));
        slotField.setValue(nonNull(ad.getAdsenseSlot()));
        activeField.setValue(ad.isActive());
        activeField.setVisible(!isNew);

        Runnable updateFields = () -> {
            AdKind kind = kindField.getValue();
            boolean adsense = kind == AdKind.ADSENSE;
            slotField.setVisible(adsense);
            titleField.setVisible(!adsense);
            textField.setVisible(!adsense);
            linkField.setVisible(!adsense);
            imageField.setVisible(!adsense);
            linkField.setHelperText(kind == AdKind.AMAZON ? "Produktlink mit deiner Partner-ID (tag=…)" : null);
        };
        kindField.addValueChangeListener(e -> updateFields.run());
        updateFields.run();

        FormLayout form = new FormLayout(kindField, audienceField, appField, titleField, textField,
                linkField, imageField, slotField, activeField);
        Responsive.twoColumns(form);
        form.setColspan(textField, 2);
        form.setColspan(linkField, 2);

        Button save = new Button("Speichern", e -> {
            ad.setKind(kindField.getValue());
            ad.setAudience(audienceField.getValue());
            ad.setApp(appField.getValue());
            boolean adsense = kindField.getValue() == AdKind.ADSENSE;
            ad.setTitle(adsense ? null : titleField.getValue());
            ad.setText(adsense ? null : textField.getValue());
            ad.setLinkUrl(adsense ? null : linkField.getValue());
            ad.setImageUrl(adsense ? null : imageField.getValue());
            ad.setAdsenseSlot(adsense ? slotField.getValue() : null);
            ad.setActive(activeField.getValue());
            try {
                ads.save(ad);
                dialog.close();
                refresh();
                Notification.show("Gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                if (isNew) {
                    // bei Fehler nicht halb befuellt in der Liste stehen lassen
                    refresh();
                }
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Abbrechen", e -> {
            dialog.close();
            refresh();
        }), save);
        Responsive.dialog(dialog, "44em");
        dialog.open();
    }

    private static String summary(BillingAd ad) {
        if (ad.getKind() == AdKind.ADSENSE) {
            return "Anzeigenblock " + nonNull(ad.getAdsenseSlot());
        }
        String title = nonNull(ad.getTitle());
        String text = nonNull(ad.getText());
        return title.isEmpty() ? text : text.isEmpty() ? title : title + " – " + text;
    }

    static String kindLabel(AdKind kind) {
        return switch (kind) {
            case HOUSE -> "Eigene Werbung";
            case AMAZON -> "Amazon-Partnerlink";
            case ADSENSE -> "Google AdSense";
        };
    }

    static String audienceLabel(AdAudience audience) {
        return audience == AdAudience.DEBTORS ? "Nur im Minus" : "Alle Benutzer";
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
