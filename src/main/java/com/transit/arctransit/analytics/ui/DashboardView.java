package com.transit.arctransit.analytics.ui;

import com.transit.arctransit.analytics.application.IncidentService;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteSummaryView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import com.transit.arctransit.common.ui.MainLayout;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "dashboard", layout = MainLayout.class)
@PageTitle("Arc Transit Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final FleetManagementService fleetService;
    private final RouteManagementService routeService;
    private final IncidentService incidentService;

    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();
    private final SplitLayout splitLayout = new SplitLayout();

    public DashboardView(FleetManagementService fleetService, RouteManagementService routeService, IncidentService incidentService) {
        this.fleetService = fleetService;
        this.routeService = routeService;
        this.incidentService = incidentService;

        setSizeFull();
        setPadding(false);

        // Top Row: 4 summary cards
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        
        Div incidentsCard = createCard("Incidents", String.valueOf(incidentService.getRecentIncidents().size()));
        incidentsCard.getStyle().set("cursor", "pointer");
        incidentsCard.addClickListener(e -> UI.getCurrent().navigate("incidents"));
        
        cards.add(
                createCard("Active Buses", String.valueOf(fleetService.searchUnits(null, PageRequest.of(0, 1)).getTotalElements())),
                createCard("Active Routes", String.valueOf(routeService.searchRoutes(null, PageRequest.of(0, 1)).getTotalElements())),
                createCard("Completed Trips", "263"),
                incidentsCard
        );

        // Middle Section: Map IFrame + Right Sidebar
        SplitLayout middleSplit = new SplitLayout();
        middleSplit.setWidthFull();
        middleSplit.setSizeFull();

        Div mapContainer = new Div();
        mapContainer.setSizeFull();
        mapContainer.getStyle().set("overflow", "hidden");

        IFrame mapIFrame = new IFrame("https://www.openstreetmap.org/export/embed.html?bbox=120.7,14.1,121.1,14.5&layer=mapnik");
        mapIFrame.setWidthFull();
        mapIFrame.getStyle()
                .set("height", "calc(100% + 45px)")
                .set("border", "none")
                .set("margin-bottom", "-45px");

        mapContainer.add(mapIFrame);

        VerticalLayout rightSidebar = new VerticalLayout();
        rightSidebar.getStyle().set("background-color", "#f9f9f9").set("border-left", "1px solid #ddd");
        rightSidebar.add(new H4("Alerts & Incident Log"));
        rightSidebar.add(new Span("Live update: Traffic occupancy and GPS data refreshed."));

        middleSplit.addToPrimary(mapContainer);
        middleSplit.addToSecondary(rightSidebar);
        middleSplit.setSplitterPosition(75);

        // Bottom Section: TabSheet with Grids and SplitLayout for side panel
        HorizontalLayout bottomHeader = new HorizontalLayout();
        bottomHeader.setWidthFull();
        bottomHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        
        Grid<RouteSummaryView> routeGrid = new Grid<>();
        Grid<FleetUnitSummaryView> fleetGrid = new Grid<>();
        
        com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
        searchField.setPlaceholder("Search routes and buses...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(e -> {
            String q = e.getValue() != null ? e.getValue().toLowerCase() : "";
            
            var routes = routeService.searchRoutes(null, PageRequest.of(0, 50)).getContent();
            if (!q.isEmpty()) {
                routes = routes.stream().filter(r -> r.routeName().toLowerCase().contains(q) || r.routeCode().toLowerCase().contains(q)).toList();
            }
            routeGrid.setItems(routes);
            
            var fleets = fleetService.searchUnits(null, PageRequest.of(0, 50)).getContent();
            if (!q.isEmpty()) {
                fleets = fleets.stream().filter(f -> f.unitNumber().toLowerCase().contains(q) || f.plateNumber().toLowerCase().contains(q)).toList();
            }
            fleetGrid.setItems(fleets);
        });
        
        bottomHeader.add(searchField);

        TabSheet bottomTabs = new TabSheet();
        bottomTabs.setSizeFull();

        routeGrid.addColumn(RouteSummaryView::routeCode).setHeader("Route Code");
        routeGrid.addColumn(RouteSummaryView::routeName).setHeader("Route Name");
        routeGrid.addColumn(RouteSummaryView::stopCount).setHeader("# Stops");
        routeGrid.setItems(routeService.searchRoutes(null, PageRequest.of(0, 50)).getContent());
        routeGrid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(route -> showDetails("Route Details", "Route: " + route.routeCode() + " - " + route.routeName()));
        });
        bottomTabs.add(new Tab("Active Bus Routes"), routeGrid);

        fleetGrid.addColumn(unit -> unit.unitNumber() + " (" + unit.plateNumber() + ")").setHeader("Unit (Plate)");
        fleetGrid.addColumn(FleetUnitSummaryView::operationalStatus).setHeader("Status");
        fleetGrid.setItems(fleetService.searchUnits(null, PageRequest.of(0, 50)).getContent());
        fleetGrid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(unit -> showDetails("Fleet Details", "Unit: " + unit.unitNumber() + "\nPlate: " + unit.plateNumber() + "\nStatus: " + unit.operationalStatus()));
        });
        bottomTabs.add(new Tab("Live Fleet Monitor"), fleetGrid);

        // Setup Side Panel
        Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> hideSidePanel());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout sidePanelHeader = new HorizontalLayout(new H3("Details"), closeButton);
        sidePanelHeader.setWidthFull();
        sidePanelHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        sidePanelHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        
        sidePanel.add(sidePanelHeader, sidePanelContent);
        sidePanel.setVisible(false);
        sidePanel.setWidth("350px");
        sidePanel.getStyle().set("background-color", "#ffffff").set("border-left", "1px solid #ddd");

        VerticalLayout primaryContent = new VerticalLayout(bottomHeader, bottomTabs);
        primaryContent.setSizeFull();
        primaryContent.setPadding(false);

        splitLayout.addToPrimary(primaryContent);
        splitLayout.addToSecondary(sidePanel);
        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();
        SplitLayout verticalSplit = new SplitLayout(middleSplit, splitLayout);
        verticalSplit.setOrientation(SplitLayout.Orientation.VERTICAL);
        verticalSplit.setSizeFull();
        verticalSplit.setSplitterPosition(50);

        add(cards, verticalSplit);
        expand(verticalSplit);
    }

    private Div createCard(String title, String value) {
        Div card = new Div();
        card.addClassNames("bg-base", "border", "border-contrast-10", "rounded-m", "p-m");
        card.getStyle().set("flex", "1").set("text-align", "center").set("border", "1px solid #ccc").set("padding", "10px");
        Span t = new Span(title);
        t.getStyle().set("display", "block").set("color", "#666");
        Span v = new Span(value);
        v.getStyle().set("display", "block").set("font-size", "24px").set("font-weight", "bold");
        card.add(t, v);
        return card;
    }

    private void showDetails(String title, String content) {
        sidePanelContent.removeAll();
        sidePanelContent.add(new Paragraph(content));
        sidePanel.setVisible(true);
        splitLayout.setSplitterPosition(75);
    }

    private void hideSidePanel() {
        sidePanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
    }
}
