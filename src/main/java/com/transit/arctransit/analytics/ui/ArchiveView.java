package com.transit.arctransit.analytics.ui;

import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverSummaryView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteSummaryView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import com.transit.arctransit.common.ui.MainLayout;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.stream.Collectors;

/**
 * Archive view to display soft-deleted records.
 */
@Route(value = "archive", layout = MainLayout.class)
@PageTitle("Archived Data")
@PermitAll
public class ArchiveView extends VerticalLayout {

    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;

    private final Grid<FleetUnitSummaryView> fleetGrid = new Grid<>();
    private final Grid<DriverSummaryView> driverGrid = new Grid<>();
    private final Grid<RouteSummaryView> routeGrid = new Grid<>();

    private final SplitLayout splitLayout = new SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();
    
    private String currentSearchFilter = "";

    public ArchiveView(FleetManagementService fleetService,
                       DriverManagementService driverService,
                       RouteManagementService routeService) {
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header and Search
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(true);

        H2 title = new H2("Archived Data");
        title.getStyle().set("margin", "0");

        TextField searchField = new TextField();
        searchField.setPlaceholder("Search archives...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearchFilter = e.getValue();
            refreshAll();
        });

        headerLayout.add(title, searchField);

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        // Archived Buses
        fleetGrid.addColumn(FleetUnitSummaryView::unitNumber).setHeader("Unit");
        fleetGrid.addColumn(FleetUnitSummaryView::plateNumber).setHeader("Plate");
        fleetGrid.addColumn(FleetUnitSummaryView::operationalStatus).setHeader("Status");
        if (isAdmin) {
            fleetGrid.addComponentColumn(unit -> createActionButtons("Fleet Unit " + unit.unitNumber(), 
                () -> { fleetService.hardDeleteUnit(unit.id()); refreshFleet(); },
                () -> { fleetService.unarchiveUnit(unit.id()); refreshFleet(); }
            )).setHeader("Actions").setAutoWidth(true);
        }
        fleetGrid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(u -> showDetails("Archived Bus", "Unit: " + u.unitNumber() + "\nPlate: " + u.plateNumber())));
        tabSheet.add(new Tab("Archived Buses"), fleetGrid);

        // Archived Drivers
        driverGrid.addColumn(DriverSummaryView::employeeNumber).setHeader("Employee #");
        driverGrid.addColumn(DriverSummaryView::fullName).setHeader("Name");
        driverGrid.addColumn(DriverSummaryView::licenseNumber).setHeader("License");
        if (isAdmin) {
            driverGrid.addComponentColumn(driver -> createActionButtons("Driver " + driver.employeeNumber(), 
                () -> { driverService.hardDeleteDriver(driver.id()); refreshDrivers(); },
                () -> { driverService.unarchiveDriver(driver.id()); refreshDrivers(); }
            )).setHeader("Actions").setAutoWidth(true);
        }
        driverGrid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(d -> showDetails("Archived Driver", "Name: " + d.fullName() + "\nLicense: " + d.licenseNumber())));
        tabSheet.add(new Tab("Archived Drivers"), driverGrid);

        // Archived Routes
        routeGrid.addColumn(RouteSummaryView::routeCode).setHeader("Route Code");
        routeGrid.addColumn(RouteSummaryView::routeName).setHeader("Route Name");
        routeGrid.addColumn(RouteSummaryView::stopCount).setHeader("# Stops");
        if (isAdmin) {
            routeGrid.addComponentColumn(route -> createActionButtons("Route " + route.routeCode(), 
                () -> { routeService.hardDeleteRoute(route.id()); refreshRoutes(); },
                () -> { routeService.unarchiveRoute(route.id()); refreshRoutes(); }
            )).setHeader("Actions").setAutoWidth(true);
        }
        routeGrid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(r -> showDetails("Archived Route", "Code: " + r.routeCode() + "\nName: " + r.routeName())));
        tabSheet.add(new Tab("Archived Routes"), routeGrid);

        VerticalLayout contentContainer = new VerticalLayout(headerLayout, tabSheet);
        contentContainer.setSizeFull();
        contentContainer.setPadding(false);

        // Side Panel setup
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

        splitLayout.addToPrimary(contentContainer);
        splitLayout.addToSecondary(sidePanel);
        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();

        add(splitLayout);
        refreshAll();
    }

    private HorizontalLayout createActionButtons(String entityName, Runnable deleteAction, Runnable unarchiveAction) {
        Button delBtn = new Button(VaadinIcon.TRASH.create());
        delBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        delBtn.setTooltipText("Hard Delete");
        delBtn.addClickListener(e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Permanently Delete " + entityName + "?");
            dialog.setText("Are you sure? This action cannot be undone and will permanently erase this record from the database.");
            dialog.setCancelable(true);
            dialog.setConfirmText("Delete Forever");
            dialog.setConfirmButtonTheme("error primary");
            dialog.addConfirmListener(event -> {
                try {
                    deleteAction.run();
                    Notification.show(entityName + " permanently deleted.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            dialog.open();
        });
        
        Button unarchiveBtn = new Button(VaadinIcon.ARROW_CIRCLE_UP.create());
        unarchiveBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
        unarchiveBtn.setTooltipText("Unarchive");
        unarchiveBtn.addClickListener(e -> {
            try {
                unarchiveAction.run();
                Notification.show(entityName + " unarchived.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return new HorizontalLayout(unarchiveBtn, delBtn);
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

    private void refreshAll() {
        refreshFleet();
        refreshDrivers();
        refreshRoutes();
    }

    private void refreshFleet() {
        var items = fleetService.searchArchivedUnits(PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(u -> u.unitNumber().toLowerCase().contains(q) || u.plateNumber().toLowerCase().contains(q)).collect(Collectors.toList());
        }
        fleetGrid.setItems(items);
    }

    private void refreshDrivers() {
        var items = driverService.searchArchivedDrivers(PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(d -> d.fullName().toLowerCase().contains(q) || d.employeeNumber().toLowerCase().contains(q)).collect(Collectors.toList());
        }
        driverGrid.setItems(items);
    }

    private void refreshRoutes() {
        var items = routeService.searchArchivedRoutes(PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(r -> r.routeCode().toLowerCase().contains(q) || r.routeName().toLowerCase().contains(q)).collect(Collectors.toList());
        }
        routeGrid.setItems(items);
    }
}
