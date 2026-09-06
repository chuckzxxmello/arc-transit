package com.transit.arctransit.analytics.ui;

// common UI
import com.transit.arctransit.common.ui.MainLayout;

// dispatch
import com.transit.arctransit.dispatch.DispatchService;

// fleet
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitSummaryView;

// incident
import com.transit.arctransit.incident.IncidentManagementService;

// maintenance
import com.transit.arctransit.maintenance.MaintenanceService;

// reconciliation
import com.transit.arctransit.reconciliation.ExceptionSummaryView;
import com.transit.arctransit.reconciliation.ReconciliationService;

// route
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteSummaryView;

// vaadin
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;

// security
import jakarta.annotation.security.RolesAllowed;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// java

import java.util.List;

// dashboard
@Route(
        value = "",
        layout = MainLayout.class
)
@RouteAlias(
        value = "dashboard",
        layout = MainLayout.class
)
@PageTitle("Arc Transit Dashboard")
@RolesAllowed({
        "SYSTEM_ADMIN",
        "OPERATIONS_STAFF"
})
public class DashboardView
        extends VerticalLayout {

    // dependencies
    private final FleetManagementService fleetService;
    private final RouteManagementService routeService;
    private final IncidentManagementService incidentService;
    private final DispatchService dispatchService;
    private final MaintenanceService maintenanceService;
    private final ReconciliationService reconciliationService;

    // summary cards
    private Div exceptionsCard;
    private Div staleCard;
    private Div maintenanceCard;
    private Div incidentsCard;

    // details panel
    private final SplitLayout splitLayout = new SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();

    // constructor
    public DashboardView(
            FleetManagementService fleetService,
            RouteManagementService routeService,
            IncidentManagementService incidentService,
            DispatchService dispatchService,
            MaintenanceService maintenanceService,
            ReconciliationService reconciliationService) {

        this.fleetService = fleetService;
        this.routeService = routeService;
        this.incidentService = incidentService;
        this.dispatchService = dispatchService;
        this.maintenanceService = maintenanceService;
        this.reconciliationService = reconciliationService;

        configureView();
        configureSummaryCards();
        configureDashboardContent();

        refreshCards();
    }

    // view configuration
    private void configureView() {

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    // summary cards
    private void configureSummaryCards() {

        HorizontalLayout cards = new HorizontalLayout();

        cards.setWidthFull();
        cards.setPadding(true);

        exceptionsCard =
                createCard(
                        "Open Exceptions",
                        "0"
                );

        staleCard =
                createCard(
                        "Stale Reports",
                        "0"
                );

        maintenanceCard =
                createCard(
                        "Maintenance-Blocked Units",
                        "0"
                );

        incidentsCard =
                createCard(
                        "Active Incidents",
                        "0"
                );

        exceptionsCard.addClickListener(
                event ->
                        UI.getCurrent()
                                .navigate(
                                        "exceptions"
                                )
        );

        staleCard.addClickListener(
                event ->
                        UI.getCurrent()
                                .navigate(
                                        "exceptions"
                                )
        );

        maintenanceCard.addClickListener(
                event ->
                        UI.getCurrent()
                                .navigate(
                                        "maintenance"
                                )
        );

        incidentsCard.addClickListener(
                event ->
                        UI.getCurrent()
                                .navigate(
                                        "incidents"
                                )
        );

        cards.add(
                exceptionsCard,
                staleCard,
                maintenanceCard,
                incidentsCard
        );

        add(cards);
    }

    // main dashboard content
    private void configureDashboardContent() {

        SplitLayout middleSplit =
                createMapSection();

        SplitLayout lowerSplit =
                createManagementSection();

        SplitLayout verticalSplit =
                new SplitLayout(
                        middleSplit,
                        lowerSplit
                );

        verticalSplit.setOrientation(
                SplitLayout.Orientation.VERTICAL
        );

        verticalSplit.setSizeFull();
        verticalSplit.setSplitterPosition(50);

        add(verticalSplit);

        expand(verticalSplit);
    }

    // map and alert section
    private SplitLayout createMapSection() {

        Div mapContainer =
                new Div();

        mapContainer.setSizeFull();

        mapContainer.getStyle()
                .set(
                        "overflow",
                        "hidden"
                );

        IFrame map =
                new IFrame(
                        "https://www.openstreetmap.org/"
                                + "export/embed.html?"
                                + "bbox=120.7,14.1,121.1,14.5"
                                + "&layer=mapnik"
                );

        map.setWidthFull();

        map.getStyle()
                .set(
                        "height",
                        "calc(100% + 45px)"
                )
                .set(
                        "border",
                        "none"
                )
                .set(
                        "margin-bottom",
                        "-45px"
                );

        mapContainer.add(map);

        VerticalLayout alerts =
                createAlertSidebar();

        SplitLayout split =
                new SplitLayout(
                        mapContainer,
                        alerts
                );

        split.setWidthFull();
        split.setSizeFull();
        split.setSplitterPosition(75);

        return split;
    }

    // alerts sidebar
    private VerticalLayout createAlertSidebar() {

        VerticalLayout sidebar =
                new VerticalLayout();

        sidebar.getStyle()
                .set(
                        "background-color",
                        "#f9f9f9"
                )
                .set(
                        "border-left",
                        "1px solid #ddd"
                );

        sidebar.add(
                new H4(
                        "Alerts & Exception Log"
                )
        );

        if (isSystemAdministrator()) {

            Button reconciliationButton =
                    new Button(
                            "Run Reconciliation",
                            event ->
                                    runReconciliation()
                    );

            reconciliationButton.addThemeVariants(
                    ButtonVariant.LUMO_PRIMARY
            );

            reconciliationButton.setWidthFull();

            sidebar.add(
                    reconciliationButton
            );
        }

        sidebar.add(
                new Paragraph(
                        "Live exception counts and system metrics "
                                + "updated on reconciliation."
                )
        );

        return sidebar;
    }

    // route and fleet section
    private SplitLayout createManagementSection() {

        Grid<RouteSummaryView> routeGrid =
                createRouteGrid();

        Grid<FleetUnitSummaryView> fleetGrid =
                createFleetGrid();

        TextField searchField =
                createManagementSearchField(
                        routeGrid,
                        fleetGrid
                );

        HorizontalLayout header =
                new HorizontalLayout(
                        searchField
                );

        header.setWidthFull();

        header.setJustifyContentMode(
                FlexComponent.JustifyContentMode.END
        );

        TabSheet tabs =
                new TabSheet();

        tabs.setSizeFull();

        tabs.add(
                new Tab("Active Bus Routes"),
                routeGrid
        );

        tabs.add(
                new Tab("Fleet Condition Monitor"),
                fleetGrid
        );

        VerticalLayout primary =
                new VerticalLayout(
                        header,
                        tabs
                );

        primary.setSizeFull();
        primary.setPadding(false);
        primary.setSpacing(false);

        configureSidePanel();

        splitLayout.setSizeFull();

        splitLayout.addToPrimary(
                primary
        );

        splitLayout.addToSecondary(
                sidePanel
        );

        splitLayout.setSplitterPosition(
                100
        );

        return splitLayout;
    }

    // route grid
    private Grid<RouteSummaryView> createRouteGrid() {

        Grid<RouteSummaryView> grid =
                new Grid<>();

        grid.setSizeFull();

        grid.addColumn(
                RouteSummaryView::routeCode
        )
        .setHeader("Route Code");

        grid.addColumn(
                RouteSummaryView::routeName
        )
        .setHeader("Route Name");

        grid.addColumn(
                RouteSummaryView::stopCount
        )
        .setHeader("# Stops");

        grid.setItems(
                routeService
                        .searchRoutes(
                                null,
                                PageRequest.of(0, 50)
                        )
                        .getContent()
        );

        grid.addSelectionListener(
                event ->
                        event.getFirstSelectedItem()
                                .ifPresent(route ->
                                        showDetails(
                                                "Route Details",
                                                "Route: "
                                                        + route.routeCode()
                                                        + " - "
                                                        + route.routeName()
                                        )
                                )
        );

        return grid;
    }

    // fleet grid
    private Grid<FleetUnitSummaryView> createFleetGrid() {

        Grid<FleetUnitSummaryView> grid =
                new Grid<>();

        grid.setSizeFull();

        grid.addColumn(
                unit ->
                        unit.unitNumber()
                                + " ("
                                + unit.plateNumber()
                                + ")"
        )
        .setHeader("Unit (Plate)");

        grid.addColumn(
                FleetUnitSummaryView::operationalStatus
        )
        .setHeader("Status");

        grid.setItems(
                fleetService
                        .searchUnits(
                                null,
                                PageRequest.of(0, 50)
                        )
                        .getContent()
        );

        grid.addSelectionListener(
                event ->
                        event.getFirstSelectedItem()
                                .ifPresent(unit ->
                                        showDetails(
                                                "Fleet Details",
                                                """
                                                Unit: %s
                                                Plate: %s
                                                Status: %s
                                                """.formatted(
                                                        unit.unitNumber(),
                                                        unit.plateNumber(),
                                                        unit.operationalStatus()
                                                )
                                        )
                                )
        );

        return grid;
    }

    // dashboard search
    private TextField createManagementSearchField(
            Grid<RouteSummaryView> routeGrid,
            Grid<FleetUnitSummaryView> fleetGrid) {

        TextField searchField =
                new TextField();

        searchField.setPlaceholder(
                "Search routes and buses..."
        );

        searchField.setPrefixComponent(
                VaadinIcon.SEARCH.create()
        );

        searchField.setClearButtonVisible(true);

        searchField.setValueChangeMode(
                ValueChangeMode.LAZY
        );

        searchField.addValueChangeListener(
                event -> {

                    String query =
                            event.getValue() == null
                                    ? ""
                                    : event.getValue()
                                            .trim()
                                            .toLowerCase();

                    refreshRouteSearch(
                            routeGrid,
                            query
                    );

                    refreshFleetSearch(
                            fleetGrid,
                            query
                    );
                }
        );

        return searchField;
    }

    // route search
    private void refreshRouteSearch(
            Grid<RouteSummaryView> grid,
            String query) {

        List<RouteSummaryView> routes =
                routeService
                        .searchRoutes(
                                null,
                                PageRequest.of(0, 50)
                        )
                        .getContent();

        if (!query.isBlank()) {

            routes = routes.stream()
                    .filter(route ->
                            route.routeCode()
                                    .toLowerCase()
                                    .contains(query)
                                    || route.routeName()
                                            .toLowerCase()
                                            .contains(query)
                    )
                    .toList();
        }

        grid.setItems(routes);
    }

    // fleet search
    private void refreshFleetSearch(
            Grid<FleetUnitSummaryView> grid,
            String query) {

        List<FleetUnitSummaryView> fleets =
                fleetService
                        .searchUnits(
                                null,
                                PageRequest.of(0, 50)
                        )
                        .getContent();

        if (!query.isBlank()) {

            fleets = fleets.stream()
                    .filter(unit ->
                            unit.unitNumber()
                                    .toLowerCase()
                                    .contains(query)
                                    || unit.plateNumber()
                                            .toLowerCase()
                                            .contains(query)
                    )
                    .toList();
        }

        grid.setItems(fleets);
    }

    // refresh dashboard metrics
    private void refreshCards() {

        ExceptionSummaryView summary =
                reconciliationService
                        .getExceptionSummary();

        long staleCount =
                summary.countByType()
                        .getOrDefault(
                                "STALE_STATE",
                                0L
                        );

        updateCardValue(
                staleCard,
                String.valueOf(
                        staleCount
                )
        );

        updateCardValue(
                exceptionsCard,
                String.valueOf(
                        summary.totalOpen()
                )
        );

        long blockedUnits =
                fleetService
                        .searchUnits(
                                null,
                                PageRequest.of(0, 1000)
                        )
                        .getContent()
                        .stream()
                        .filter(unit ->
                                "UNDER_MAINTENANCE"
                                        .equals(
                                                unit.operationalStatus()
                                        )
                        )
                        .count();

        updateCardValue(
                maintenanceCard,
                String.valueOf(
                        blockedUnits
                )
        );

        updateCardValue(
                incidentsCard,
                String.valueOf(
                        incidentService
                                .countOpenIncidents()
                )
        );
    }

    // run reconciliation
    private void runReconciliation() {

        try {

            reconciliationService
                    .runReconciliation();

            refreshCards();

            showSuccess(
                    "Reconciliation run completed."
            );

        } catch (RuntimeException exception) {

            showError(
                    exception.getMessage()
            );
        }
    }

    // create metric card

    private Div createCard(
            String title,
            String value) {

        Div card =
                new Div();

        card.addClassNames(
                "bg-base",
                "border",
                "border-contrast-10",
                "rounded-m",
                "p-m"
        );

        card.getStyle()
                .set(
                        "flex",
                        "1"
                )
                .set(
                        "text-align",
                        "center"
                )
                .set(
                        "border",
                        "1px solid #ccc"
                )
                .set(
                        "padding",
                        "10px"
                );

        Span titleText =
                new Span(title);

        titleText.getStyle()
                .set(
                        "display",
                        "block"
                )
                .set(
                        "color",
                        "#666"
                );

        Span valueText =
                new Span(value);

        valueText.getStyle()
                .set(
                        "display",
                        "block"
                )
                .set(
                        "font-size",
                        "24px"
                )
                .set(
                        "font-weight",
                        "bold"
                );

        updateValueStyle(
                valueText,
                value
        );

        card.add(
                titleText,
                valueText
        );

        card.getStyle()
                .set(
                        "cursor",
                        "pointer"
                );

        return card;
    }

    // update card value

    private void updateCardValue(
            Div card,
            String value) {

        Span valueText =
                (Span) card.getComponentAt(1);

        valueText.setText(value);

        updateValueStyle(
                valueText,
                value
        );
    }

    // update card state

    private void updateValueStyle(
            Span valueText,
            String value) {

        if (!"0".equals(value)) {

            valueText.addClassNames(
                    LumoUtility.TextColor.ERROR
            );

        } else {

            valueText.removeClassName(
                    LumoUtility.TextColor.ERROR
            );
        }
    }

    // check administrator role

    private boolean isSystemAdministrator() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return false;
        }

        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_SYSTEM_ADMIN"
                                .equals(
                                        authority.getAuthority()
                                )
                );
    }

    // details panel

    private void configureSidePanel() {

        Button closeButton =
                new Button(
                        VaadinIcon.CLOSE.create(),
                        event ->
                                hideSidePanel()
                );

        closeButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY
        );

        HorizontalLayout header =
                new HorizontalLayout(
                        new H3("Details"),
                        closeButton
                );

        header.setWidthFull();

        header.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        header.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        sidePanel.add(
                header,
                sidePanelContent
        );

        sidePanel.setVisible(false);
        sidePanel.setWidth("350px");

        sidePanel.getStyle()
                .set(
                        "background-color",
                        "#ffffff"
                )
                .set(
                        "border-left",
                        "1px solid #ddd"
                );
    }

    // show details

    private void showDetails(
            String title,
            String content) {

        sidePanelContent.removeAll();

        sidePanelContent.add(
                new H3(title),
                new Paragraph(content)
        );

        sidePanel.setVisible(true);

        splitLayout.setSplitterPosition(
                75
        );
    }

    // hide details

    private void hideSidePanel() {

        sidePanel.setVisible(false);

        splitLayout.setSplitterPosition(
                100
        );
    }

    // success notification

    private void showSuccess(
            String message) {

        Notification.show(
                        message,
                        3000,
                        Notification.Position.TOP_END
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_SUCCESS
                );
    }

    // error notification

    private void showError(
            String message) {

        Notification.show(
                        message == null
                                || message.isBlank()
                                ? "The operation could not be completed."
                                : message,
                        5000,
                        Notification.Position.TOP_END
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }
}