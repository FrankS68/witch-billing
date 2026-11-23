package de.witchcafe.billing.server.ui;

import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.auth.CurrentUser;
import de.witchcafe.auth.domain.AppUser;
import de.witchcafe.auth.domain.AppUserRepository;
import de.witchcafe.billing.server.ledger.BillingAccount;
import de.witchcafe.billing.server.ledger.BillingEntry;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.RolesAllowed;

/**
 * Admin: Konten suchen, Buchungen eines Kontos ansehen, manuell Guthaben buchen
 * (z.B. fuer erbrachte Dienstleistungen) oder korrigieren.
 * <p>
 * Namen sind nur fuer Benutzer bekannt, die sich auch hier am Billing-Dienst angemeldet
 * haben; sonst steht nur die UserId da.
 */
@Route("admin/konten")
@PageTitle("Konten")
@Menu(order = 90, icon = "vaadin:book-dollar", title = "Konten")
@RolesAllowed(AppRoles.ADMIN)
public class AccountsAdminView extends VerticalLayout {

    private static final int MAX_ACCOUNTS = 500;

    private final BillingService billing;
    private final CurrentUser currentUser;
    private final Map<String, String> namesByUserId;

    private final TextField search = new TextField();
    private final Grid<BillingAccount> accountGrid = new Grid<>(BillingAccount.class, false);
    private final Grid<BillingEntry> entryGrid = Format.entryGrid();
    private final H3 entryTitle = new H3();

    public AccountsAdminView(BillingService billing, CurrentUser currentUser, AppUserRepository users) {
        this.billing = billing;
        this.currentUser = currentUser;
        this.namesByUserId = users.findAll().stream()
                .collect(Collectors.toMap(AppUser::getUserId, AppUser::getName, (a, b) -> a));
        setSizeFull();

        search.setPlaceholder("E-Mail, Handle, Name oder UserId");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(e -> refreshAccounts());
        Responsive.maxWidth(search, "18em");

        Button book = new Button("Buchen", VaadinIcon.PLUS.create(), e -> openBookingDialog(
                accountGrid.asSingleSelect().getOptionalValue().map(BillingAccount::getUserId).orElse("")));
        book.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Die Liste laedt nur beim Oeffnen; neue Konten aus den Apps erscheinen erst hiermit (oder per Reload)
        Button reload = new Button("Aktualisieren", VaadinIcon.REFRESH.create(), e -> reload());

        accountGrid.addColumn(a -> a.getLabel() == null ? "" : a.getLabel()).setHeader("E-Mail / Handle").setAutoWidth(true);
        var nameColumn = accountGrid.addColumn(this::name).setHeader("Name").setAutoWidth(true);
        var idColumn = accountGrid.addColumn(BillingAccount::getUserId).setHeader("UserId").setAutoWidth(true);
        accountGrid.addComponentColumn(a -> Format.creditsBadge(a.getBalance())).setHeader("Kontostand")
                .setAutoWidth(true).setFlexGrow(0);
        var sinceColumn = accountGrid.addColumn(a -> Format.dateTime(a.getCreatedAt())).setHeader("Seit")
                .setAutoWidth(true).setFlexGrow(0);
        Responsive.hideOnMobile(accountGrid, nameColumn, idColumn, sinceColumn);
        accountGrid.asSingleSelect().addValueChangeListener(e -> showEntries(e.getValue()));
        accountGrid.setHeight("40%");
        // 40 % bleiben bei niedrigen Fenstern sonst nur eine Zeile, weitere Konten sind dann unsichtbar
        accountGrid.setMinHeight("16em");

        entryGrid.setSizeFull();
        entryTitle.getStyle().set("margin", "0");

        // Suchfeld und Knoepfe in der ViewToolbar: witch-ui holt sie in die obere Leiste
        add(new ViewToolbar("Konten", search, book, reload), accountGrid, entryTitle, entryGrid);
        refreshAccounts();
        showEntries(null);
    }

    private void refreshAccounts() {
        accountGrid.setItems(billing.accounts(search.getValue(), MAX_ACCOUNTS));
    }

    /** Konten neu laden; das gewaehlte Konto bleibt ausgewaehlt und seine Buchungen werden neu geholt. */
    private void reload() {
        String selected = accountGrid.asSingleSelect().getOptionalValue().map(BillingAccount::getUserId).orElse(null);
        refreshAccounts();
        accountGrid.getListDataView().getItems()
                .filter(a -> a.getUserId().equals(selected)).findFirst()
                .ifPresentOrElse(accountGrid::select, () -> showEntries(null));
    }

    private void showEntries(BillingAccount account) {
        if (account == null) {
            entryTitle.setText("Konto auswählen, um die Buchungen zu sehen");
            entryGrid.setItems();
            return;
        }
        String name = name(account);
        String id = account.getLabel() != null ? account.getLabel() : account.getUserId();
        entryTitle.setText("Buchungen von " + (name.isEmpty() ? id : name + " (" + id + ")"));
        entryGrid.setItems(billing.recentEntries(account.getUserId(), MyCreditsView.MAX_ENTRIES));
    }

    private void openBookingDialog(String userId) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Guthaben buchen");

        TextField userField = new TextField("E-Mail / Handle oder UserId (provider:id)");
        userField.setValue(userId);
        userField.setRequired(true);
        userField.setHelperText("E-Mail bzw. Handle nur für bestehende Konten. Neue Konten entstehen beim ersten Buchen (mit UserId), mit Startguthaben.");

        IntegerField creditsField = new IntegerField("Credits");
        creditsField.setHelperText("Positiv = Gutschrift, negativ = Korrektur");
        creditsField.setStepButtonsVisible(true);
        creditsField.setRequired(true);

        TextArea noteField = new TextArea("Begründung");
        noteField.setPlaceholder("z.B. 20 Quizfragen Korrektur gelesen");
        noteField.setMaxLength(500);
        noteField.setRequired(true);

        FormLayout form = new FormLayout(userField, creditsField, noteField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button save = new Button("Buchen", e -> {
            Integer credits = creditsField.getValue();
            if (userField.isEmpty() || credits == null || credits == 0 || noteField.getValue().isBlank()) {
                Notification.show("Bitte Konto, Credits (nicht 0) und Begründung angeben");
                return;
            }
            try {
                String target = billing.resolveUserId(userField.getValue());
                var booking = billing.adminBooking(target, credits, noteField.getValue().trim(),
                        currentUser.require().getUserId().toString());
                dialog.close();
                Notification n = Notification.show(Format.signed(credits) + " für " + label(target)
                        + " gebucht, neuer Stand " + booking.balanceAfter(), 4000, Notification.Position.BOTTOM_END);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshAccounts();
                accountGrid.getListDataView().getItems()
                        .filter(a -> a.getUserId().equals(target)).findFirst()
                        .ifPresentOrElse(accountGrid::select, () -> showEntries(null));
            } catch (IllegalArgumentException ex) {
                Notification n = Notification.show(ex.getMessage());
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Abbrechen", e -> dialog.close()), save);
        Responsive.dialog(dialog, "32em");
        dialog.open();
        (userId.isEmpty() ? userField : creditsField).focus();
    }

    /** Name aus der App, sonst aus der Benutzerverwaltung des Billing-Dienstes. */
    private String name(BillingAccount account) {
        if (account.getDisplayName() != null) {
            return account.getDisplayName();
        }
        return namesByUserId.getOrDefault(account.getUserId(), "");
    }

    private String label(String userId) {
        String name = namesByUserId.get(userId);
        return name == null ? userId : name + " (" + userId + ")";
    }
}
