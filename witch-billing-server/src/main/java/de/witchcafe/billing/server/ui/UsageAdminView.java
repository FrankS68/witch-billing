package de.witchcafe.billing.server.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.witchcafe.auth.AppRoles;
import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.billing.server.ledger.UsageSummary;
import de.witchcafe.ui.Responsive;
import de.witchcafe.ui.ViewToolbar;
import jakarta.annotation.security.RolesAllowed;

/**
 * Admin: Wie oft wurde welches Feature genutzt bzw. welcher Beitrag gutgeschrieben,
 * von wie vielen Benutzern, mit wie vielen Credits - pro Zeitraum.
 */
@Route("admin/nutzung")
@PageTitle("Nutzung")
@Menu(order = 92, icon = "vaadin:chart", title = "Nutzung")
@RolesAllowed(AppRoles.ADMIN)
public class UsageAdminView extends VerticalLayout {

    private static final Map<String, Duration> PERIODS = new LinkedHashMap<>();
    static {
        PERIODS.put("Letzte 24 Stunden", Duration.ofDays(1));
        PERIODS.put("Letzte 7 Tage", Duration.ofDays(7));
        PERIODS.put("Letzte 30 Tage", Duration.ofDays(30));
        PERIODS.put("Letztes Jahr", Duration.ofDays(365));
    }

    private final BillingService billing;
    private final Grid<UsageSummary> grid = new Grid<>(UsageSummary.class, false);
    private final Select<String> period = new Select<>();

    public UsageAdminView(BillingService billing) {
        this.billing = billing;
        setSizeFull();

        period.setItems(PERIODS.keySet());
        period.setValue("Letzte 30 Tage");
        period.addValueChangeListener(e -> refresh());

        grid.addColumn(UsageSummary::app).setHeader("App").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(UsageSummary::feature).setHeader("Feature").setAutoWidth(true);
        grid.addColumn(s -> Format.kind(s.kind())).setHeader("Art").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(UsageSummary::bookings).setHeader("Buchungen").setAutoWidth(true).setFlexGrow(0);
        var quantityColumn = grid.addColumn(UsageSummary::quantity).setHeader("Menge").setAutoWidth(true).setFlexGrow(0);
        var usersColumn = grid.addColumn(UsageSummary::users).setHeader("Benutzer").setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(s -> Format.creditsBadge(s.credits() == null ? 0 : s.credits()))
                .setHeader("Credits").setAutoWidth(true).setFlexGrow(0);
        Responsive.hideOnMobile(grid, quantityColumn, usersColumn);
        grid.setSizeFull();

        add(new ViewToolbar("Nutzung", period), grid);
        refresh();
    }

    private void refresh() {
        Instant since = Instant.now().minus(PERIODS.get(period.getValue()));
        grid.setItems(billing.usageSince(since));
    }
}
