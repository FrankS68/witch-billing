package de.witchcafe.billing.server.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.billing.server.ledger.BillingPrice;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.billing.server.ledger.PriceKind;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.RolesAllowed;

/**
 * Admin: Preisliste pflegen. Pro App und Feature ein Preis (Verbrauch kostet Credits)
 * oder eine Belohnung (Beitrag bringt Credits). App, Feature und Art sind nach dem
 * Anlegen fest; nicht mehr benoetigte Eintraege werden deaktiviert statt geloescht.
 */
@Route("admin/preise")
@PageTitle("Preisliste")
@Menu(order = 91, icon = "vaadin:price-tag", title = "Preisliste")
@RolesAllowed(AppRoles.ADMIN)
public class PricesAdminView extends VerticalLayout {

    private final BillingService billing;
    private final Grid<BillingPrice> grid = new Grid<>(BillingPrice.class, false);

    public PricesAdminView(BillingService billing) {
        this.billing = billing;
        setSizeFull();

        Button create = new Button("Neuer Eintrag", VaadinIcon.PLUS.create(), e -> openDialog(null));
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addColumn(BillingPrice::getApp).setHeader("App").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(BillingPrice::getFeature).setHeader("Feature").setAutoWidth(true);
        grid.addColumn(p -> kindLabel(p.getKind())).setHeader("Art").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(p -> Format.creditsBadge(p.getKind() == PriceKind.COST
                ? -p.getCreditsPerUnit() : p.getCreditsPerUnit()))
                .setHeader("Credits/Einheit").setAutoWidth(true).setFlexGrow(0);
        var descColumn = grid.addColumn(p -> p.getDescription() == null ? "" : p.getDescription())
                .setHeader("Beschreibung").setFlexGrow(1);
        grid.addComponentColumn(p -> {
            Span badge = new Span(p.isActive() ? "Aktiv" : "Aus");
            badge.getElement().getThemeList().add(p.isActive() ? "badge success" : "badge contrast");
            return badge;
        }).setHeader("Status").setAutoWidth(true).setFlexGrow(0);
        grid.addItemClickListener(e -> openDialog(e.getItem()));
        Responsive.hideOnMobile(grid, descColumn);
        grid.setSizeFull();

        add(new ViewToolbar("Preisliste", create), grid);
        refresh();
    }

    private void refresh() {
        grid.setItems(billing.prices());
    }

    /** Dialog zum Anlegen ({@code price == null}) oder Bearbeiten. */
    private void openDialog(BillingPrice price) {
        boolean isNew = price == null;
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Neuer Eintrag" : price.getApp() + " / " + price.getFeature());

        TextField appField = new TextField("App");
        appField.setPlaceholder("quiz, games, garten");
        TextField featureField = new TextField("Feature");
        featureField.setPlaceholder("z.B. ki-fragen");
        Select<PriceKind> kindField = new Select<>();
        kindField.setLabel("Art");
        kindField.setItems(PriceKind.values());
        kindField.setItemLabelGenerator(PricesAdminView::kindLabel);
        IntegerField creditsField = new IntegerField("Credits pro Einheit");
        creditsField.setMin(0);
        creditsField.setHelperText("0 = nur zählen");
        TextField descField = new TextField("Beschreibung");
        descField.setMaxLength(255);
        Checkbox activeField = new Checkbox("Aktiv");

        if (isNew) {
            kindField.setValue(PriceKind.COST);
            creditsField.setValue(1);
            activeField.setValue(true);
            activeField.setVisible(false);
        } else {
            appField.setValue(price.getApp());
            featureField.setValue(price.getFeature());
            kindField.setValue(price.getKind());
            creditsField.setValue((int) price.getCreditsPerUnit());
            descField.setValue(price.getDescription() == null ? "" : price.getDescription());
            activeField.setValue(price.isActive());
            appField.setReadOnly(true);
            featureField.setReadOnly(true);
            kindField.setReadOnly(true);
        }

        FormLayout form = new FormLayout(appField, featureField, kindField, creditsField, descField, activeField);
        Responsive.twoColumns(form);

        Button save = new Button("Speichern", e -> {
            Integer credits = creditsField.getValue();
            if (credits == null || credits < 0) {
                Notification.show("Credits pro Einheit müssen 0 oder mehr sein");
                return;
            }
            String desc = descField.getValue().isBlank() ? null : descField.getValue().trim();
            try {
                if (isNew) {
                    billing.createPrice(appField.getValue(), featureField.getValue(), kindField.getValue(), credits, desc);
                } else {
                    billing.updatePrice(price.getId(), credits, desc, activeField.getValue());
                }
                dialog.close();
                refresh();
                Notification.show("Gespeichert").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IllegalArgumentException ex) {
                Notification.show(ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), save);
        Responsive.dialog(dialog, "40em");
        dialog.open();
    }

    static String kindLabel(PriceKind kind) {
        return kind == PriceKind.COST ? "Verbrauch (kostet)" : "Beitrag (bringt)";
    }
}
