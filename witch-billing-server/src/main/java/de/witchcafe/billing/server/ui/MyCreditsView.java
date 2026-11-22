package de.witchcafe.billing.server.ui;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.CurrentUser;
import de.witchcafe.billing.server.ledger.BillingEntry;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.PermitAll;

/**
 * "Mein Guthaben": Kontostand und die letzten Buchungen des angemeldeten Benutzers.
 */
@Route("")
@PageTitle("Mein Guthaben")
@Menu(order = 0, icon = "vaadin:wallet", title = "Mein Guthaben")
@PermitAll
public class MyCreditsView extends VerticalLayout {

    static final int MAX_ENTRIES = 200;

    public MyCreditsView(CurrentUser currentUser, BillingService billing) {
        setSizeFull();
        String userId = currentUser.require().getUserId().toString();

        H2 balance = new H2(billing.balance(userId) + " Credits");
        balance.getStyle().set("margin", "0");

        Paragraph hint = new Paragraph("Credits verbrauchst du in Quiz, Games und DerGarten für "
                + "kostenpflichtige Funktionen. Du bekommst neue Credits für Beiträge in den Apps "
                + "oder für Dienstleistungen, die ein Admin dir gutschreibt.");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        Grid<BillingEntry> grid = Format.entryGrid();
        grid.setItems(billing.recentEntries(userId, MAX_ENTRIES));
        grid.setSizeFull();

        add(new ViewToolbar("Mein Guthaben"), balance, hint, grid);
    }
}
