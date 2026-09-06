package com.transit.arctransit.analytics.ui;

// fleet
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitSummaryView;

// driver
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverSummaryView;

// route
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteSummaryView;

// dispatch
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchService;

// common UI
import com.transit.arctransit.common.ui.MainLayout;

// vaadin
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

// security
import jakarta.annotation.security.RolesAllowed;

// spring
import org.springframework.data.domain.PageRequest;

// java
import java.util.List;

// archive management view
@Route(
        value = "archive",
        layout = MainLayout.class
)
@PageTitle("Archived Data")
@RolesAllowed("SYSTEM_ADMIN")
public class ArchiveView extends VerticalLayout {

    // dependencies
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final DispatchService dispatchService;

    // grids
    private final Grid<FleetUnitSummaryView> fleetGrid = new Grid<>();
    private final Grid<DriverSummaryView> driverGrid = new Grid<>();
    private final Grid<RouteSummaryView> routeGrid = new Grid<>();
    private final Grid<DispatchAssignmentView> dispatchGrid = new Grid<>();

    // side panel
    private final SplitLayout splitLayout = new SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final Div sidePanelContent = new Div();

    // current search text
    private String currentSearchFilter = "";

    // constructor
    public ArchiveView(
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService,
            DispatchService dispatchService) {

        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;

        configureView();
        configureArchiveTabs();
        configureSidePanel();

        add(splitLayout);

        expand(splitLayout);

        refreshAll();
    }

    // view configuration
    private void configureView() {

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    // archive tabs
    private void configureArchiveTabs() {

        TabSheet tabSheet =
                new TabSheet();

        tabSheet.setSizeFull();

        configureFleetGrid();
        configureDriverGrid();
        configureRouteGrid();
        configureDispatchGrid();

        tabSheet.add(
                new Tab("Archived Buses"),
                fleetGrid
        );

        tabSheet.add(
                new Tab("Archived Drivers"),
                driverGrid
        );

        tabSheet.add(
                new Tab("Archived Routes"),
                routeGrid
        );

        tabSheet.add(
                new Tab("Archived Dispatches"),
                dispatchGrid
        );

        HorizontalLayout header =
                createHeader();

        VerticalLayout primaryContent =
                new VerticalLayout(
                        header,
                        tabSheet
                );

        primaryContent.setSizeFull();
        primaryContent.setPadding(false);
        primaryContent.setSpacing(false);

        splitLayout.addToPrimary(
                primaryContent
        );

        splitLayout.addToSecondary(
                sidePanel
        );

        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();
    }

    // archive header
    private HorizontalLayout createHeader() {

        H2 title =
                new H2("Archived Data");

        title.getStyle()
                .set("margin", "0");

        TextField searchField =
                new TextField();

        searchField.setPlaceholder(
                "Search current archive page..."
        );

        searchField.setPrefixComponent(
                VaadinIcon.SEARCH.create()
        );

        searchField.setClearButtonVisible(true);

        searchField.setValueChangeMode(
                ValueChangeMode.LAZY
        );

        searchField.addValueChangeListener(event -> {

            currentSearchFilter =
                    normalizeSearchFilter(
                            event.getValue()
                    );

            refreshAll();
        });

        HorizontalLayout header =
                new HorizontalLayout(
                        title,
                        searchField
                );

        header.setWidthFull();

        header.setPadding(true);

        header.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        header.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        return header;
    }

    // fleet grid
    private void configureFleetGrid() {

        fleetGrid.setSizeFull();

        fleetGrid.addColumn(
                FleetUnitSummaryView::unitNumber
        )
        .setHeader("Unit");

        fleetGrid.addColumn(
                FleetUnitSummaryView::plateNumber
        )
        .setHeader("Plate");

        fleetGrid.addColumn(
                FleetUnitSummaryView::operationalStatus
        )
        .setHeader("Status");

        fleetGrid.addComponentColumn(
                unit ->
                        createActionButtons(
                                "Fleet Unit "
                                        + unit.unitNumber(),
                                () -> {
                                    fleetService
                                            .hardDeleteUnit(
                                                    unit.id()
                                            );

                                    refreshFleet();
                                },
                                () -> {
                                    fleetService
                                            .unarchiveUnit(
                                                    unit.id()
                                            );

                                    refreshFleet();
                                }
                        )
        )
        .setHeader("Actions");

        fleetGrid.addSelectionListener(
                event ->
                        event.getFirstSelectedItem()
                                .ifPresent(unit ->
                                        showDetails(
                                                "Archived Bus",
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
    }

    // driver grid
    private void configureDriverGrid() {

        driverGrid.setSizeFull();

        driverGrid.addColumn(
                DriverSummaryView::employeeNumber
        )
        .setHeader("Employee #");

        driverGrid.addColumn(
                DriverSummaryView::fullName
        )
        .setHeader("Name");

        driverGrid.addColumn(
                DriverSummaryView::licenseNumber
        )
        .setHeader("License");

        driverGrid.addComponentColumn(
                driver ->
                        createActionButtons(
                                "Driver "
                                        + driver.employeeNumber(),
                                () -> {
                                    driverService
                                            .hardDeleteDriver(
                                                    driver.id()
                                            );

                                    refreshDrivers();
                                },
                                () -> {
                                    driverService
                                            .unarchiveDriver(
                                                    driver.id()
                                            );

                                    refreshDrivers();
                                }
                        )
        )
        .setHeader("Actions");

        driverGrid.addSelectionListener(
                event ->
                        event.getFirstSelectedItem()
                                .ifPresent(driver ->
                                        showDetails(
                                                "Archived Driver",
                                                """
                                                Name: %s
                                                License: %s
                                                """.formatted(
                                                        driver.fullName(),
                                                        driver.licenseNumber()
                                                )
                                        )
                                )
        );
    }

    // route grid
    private void configureRouteGrid() {

        routeGrid.setSizeFull();

        routeGrid.addColumn(
                RouteSummaryView::routeCode
        )
        .setHeader("Route Code");

        routeGrid.addColumn(
                RouteSummaryView::routeName
        )
        .setHeader("Route Name");

        routeGrid.addColumn(
                RouteSummaryView::stopCount
        )
        .setHeader("# Stops");

        routeGrid.addComponentColumn(
                route ->
                        createActionButtons(
                                "Route "
                                        + route.routeCode(),
                                () -> {
                                    routeService
                                            .hardDeleteRoute(
                                                    route.id()
                                            );

                                    refreshRoutes();
                                },
                                () -> {
                                    routeService
                                            .unarchiveRoute(
                                                    route.id()
                                            );

                                    refreshRoutes();
                                }
                        )
        )
        .setHeader("Actions");

        routeGrid.addSelectionListener(
                event ->
                        event.getFirstSelectedItem()
                                .ifPresent(route ->
                                        showDetails(
                                                "Archived Route",
                                                """
                                                Code: %s
                                                Name: %s
                                                """.formatted(
                                                        route.routeCode(),
                                                        route.routeName()
                                                )
                                        )
                                )
        );
    }

    // dispatch grid
    private void configureDispatchGrid() {

        dispatchGrid.setSizeFull();

        dispatchGrid.addColumn(
                DispatchAssignmentView::fleetUnitNumber
        )
        .setHeader("Bus");

        dispatchGrid.addColumn(
                DispatchAssignmentView::driverName
        )
        .setHeader("Driver");

        dispatchGrid.addColumn(
                DispatchAssignmentView::routeCode
        )
        .setHeader("Route");

        dispatchGrid.addColumn(
                DispatchAssignmentView::dispatchStatus
        )
        .setHeader("Status");

        dispatchGrid.addComponentColumn(
                dispatch ->
                        createActionButtons(
                                "Dispatch Assignment #"
                                        + dispatch.id(),
                                null,
                                () -> {
                                    dispatchService
                                            .unarchiveAssignment(
                                                    dispatch.id()
                                            );

                                    refreshDispatches();
                                }
                        )
        )
        .setHeader("Actions");

        dispatchGrid.addSelectionListener(
                event ->
                        event.getFirstSelectedItem()
                                .ifPresent(dispatch ->
                                        showDetails(
                                                "Archived Dispatch",
                                                """
                                                Bus: %s
                                                Driver: %s
                                                Route: %s
                                                Status: %s
                                                """.formatted(
                                                        dispatch.fleetUnitNumber(),
                                                        dispatch.driverName(),
                                                        dispatch.routeCode(),
                                                        dispatch.dispatchStatus()
                                                )
                                        )
                                )
        );
    }

    // action buttons
    private HorizontalLayout createActionButtons(
            String entityName,
            Runnable deleteAction,
            Runnable unarchiveAction) {

        HorizontalLayout actions =
                new HorizontalLayout();

        if (unarchiveAction != null) {

            Button unarchiveButton =
                    new Button(
                            VaadinIcon.ARROW_CIRCLE_UP.create()
                    );

            unarchiveButton.addThemeVariants(
                    ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_TERTIARY
            );

            unarchiveButton.setTooltipText(
                    "Unarchive"
            );

            unarchiveButton.addClickListener(
                    event -> {

                        try {

                            unarchiveAction.run();

                            showSuccess(
                                    entityName
                                            + " unarchived."
                            );

                        } catch (RuntimeException exception) {

                            showError(
                                    exception
                                            .getMessage()
                            );
                        }
                    }
            );

            actions.add(
                    unarchiveButton
            );
        }

        if (deleteAction != null) {

            Button deleteButton =
                    new Button(
                            VaadinIcon.TRASH.create()
                    );

            deleteButton.addThemeVariants(
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY
            );

            deleteButton.setTooltipText(
                    "Hard Delete"
            );

            deleteButton.addClickListener(
                    event ->
                            openDeleteDialog(
                                    entityName,
                                    deleteAction
                            )
            );

            actions.add(
                    deleteButton
            );
        }

        return actions;
    }

    // hard delete confirmation
    private void openDeleteDialog(
            String entityName,
            Runnable deleteAction) {

        ConfirmDialog dialog =
                new ConfirmDialog();

        dialog.setHeader(
                "Permanently Delete "
                        + entityName
                        + "?"
        );

        dialog.setText(
                "This action cannot be undone "
                        + "and will permanently erase "
                        + "the record from the database."
        );

        dialog.setCancelable(true);

        dialog.setConfirmText(
                "Delete Forever"
        );

        dialog.setConfirmButtonTheme(
                "error primary"
        );

        dialog.addConfirmListener(
                event -> {

                    try {

                        deleteAction.run();

                        showSuccess(
                                entityName
                                        + " permanently deleted."
                        );

                    } catch (RuntimeException exception) {

                        showError(
                                exception
                                        .getMessage()
                        );
                    }
                }
        );

        dialog.open();
    }

    // details panel
    private void showDetails(
            String title,
            String content) {

        sidePanelContent.removeAll();

        sidePanelContent.add(
                new H3(title),
                new Paragraph(content)
        );

        sidePanel.setVisible(true);

        splitLayout.setSplitterPosition(75);
    }

    // side panel
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

    // hide side panel
    private void hideSidePanel() {

        sidePanel.setVisible(false);

        splitLayout.setSplitterPosition(100);
    }

    // refresh all archives
    private void refreshAll() {

        refreshFleet();
        refreshDrivers();
        refreshRoutes();
        refreshDispatches();
    }

    // refresh fleet archive
    private void refreshFleet() {

        List<FleetUnitSummaryView> items =
                fleetService
                        .searchArchivedUnits(
                                PageRequest.of(0, 100)
                        )
                        .getContent();

        fleetGrid.setItems(
                filterFleet(items)
        );
    }

    // refresh driver archive
    private void refreshDrivers() {

        List<DriverSummaryView> items =
                driverService
                        .searchArchivedDrivers(
                                PageRequest.of(0, 100)
                        )
                        .getContent();

        driverGrid.setItems(
                filterDrivers(items)
        );
    }

    // refresh route archive
    private void refreshRoutes() {

        List<RouteSummaryView> items =
                routeService
                        .searchArchivedRoutes(
                                PageRequest.of(0, 100)
                        )
                        .getContent();

        routeGrid.setItems(
                filterRoutes(items)
        );
    }

    // refresh dispatch archive
    private void refreshDispatches() {

        List<DispatchAssignmentView> items =
                dispatchService
                        .searchArchivedAssignments(
                                PageRequest.of(0, 100)
                        )
                        .getContent();

        dispatchGrid.setItems(
                filterDispatches(items)
        );
    }

    // filter fleet records
    private List<FleetUnitSummaryView>
    filterFleet(
            List<FleetUnitSummaryView> items) {

        if (currentSearchFilter.isBlank()) {
            return items;
        }

        return items.stream()
                .filter(unit ->
                        contains(
                                unit.unitNumber()
                        )
                        || contains(
                                unit.plateNumber()
                        )
                )
                .toList();
    }

    // filter drivers
    private List<DriverSummaryView>
    filterDrivers(
            List<DriverSummaryView> items) {

        if (currentSearchFilter.isBlank()) {
            return items;
        }

        return items.stream()
                .filter(driver ->
                        contains(
                                driver.fullName()
                        )
                        || contains(
                                driver.employeeNumber()
                        )
                )
                .toList();
    }

    // filter routes
    private List<RouteSummaryView>
    filterRoutes(
            List<RouteSummaryView> items) {

        if (currentSearchFilter.isBlank()) {
            return items;
        }

        return items.stream()
                .filter(route ->
                        contains(
                                route.routeCode()
                        )
                        || contains(
                                route.routeName()
                        )
                )
                .toList();
    }

    // filter dispatch assignments
    private List<DispatchAssignmentView>
    filterDispatches(
            List<DispatchAssignmentView> items) {

        if (currentSearchFilter.isBlank()) {
            return items;
        }

        return items.stream()
                .filter(dispatch ->
                        contains(
                                dispatch.fleetUnitNumber()
                        )
                        || contains(
                                dispatch.driverName()
                        )
                        || contains(
                                dispatch.routeCode()
                        )
                )
                .toList();
    }

    // generic case-insensitive filter
    private boolean contains(
            String value) {

        return value != null
                && value
                        .toLowerCase()
                        .contains(
                                currentSearchFilter
                                        .toLowerCase()
                        );
    }

    // normalize search text
    private static String normalizeSearchFilter(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }

    // success notification
    private void showSuccess(
            String message) {

        Notification.show(
                        message,
                        3000,
                        Notification.Position.MIDDLE
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
                        Notification.Position.MIDDLE
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }
}