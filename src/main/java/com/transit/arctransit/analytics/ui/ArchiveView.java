package com.transit.arctransit.analytics.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.vaadin.flow.component.dialog.Dialog;
import com.transit.arctransit.common.ui.Dialogs;
// fleet
import com.transit.arctransit.common.ui.FormInputs;

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
import com.transit.arctransit.auth.UserAdministrationService;
import com.transit.arctransit.auth.UserQuery;
import com.transit.arctransit.auth.UserSummaryView;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.incident.IncidentView;
import com.transit.arctransit.maintenance.MaintenanceService;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.reporting.OperationalReportView;
import com.transit.arctransit.reporting.ReportVerificationView;
import com.transit.arctransit.common.ui.ApplicationErrors;

// common UI
import com.transit.arctransit.common.ui.MainLayout;

// vaadin
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.transit.arctransit.common.ui.ConfirmationDialog;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
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
import org.springframework.data.domain.Sort;

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
    private final UserAdministrationService userService;
    private final IncidentManagementService incidentService;
    private final MaintenanceService maintenanceService;
    private final OperationalReportService reportService;

    // grids
    private final Grid<FleetUnitSummaryView> fleetGrid = DataGrids.create();
    private final Grid<DriverSummaryView> driverGrid = DataGrids.create();
    private final Grid<RouteSummaryView> routeGrid = DataGrids.create();
    private final Grid<DispatchAssignmentView> dispatchGrid = DataGrids.create();
    private final Grid<UserSummaryView> userGrid = DataGrids.create();
    private final Grid<IncidentView> incidentGrid = DataGrids.create();
    private final Grid<MaintenanceRecordView> maintenanceGrid = DataGrids.create();
    private final Grid<OperationalReportView> reportGrid = DataGrids.create();

    // side panel
    private final Dialog detailsDialog = Dialogs.details("Archived Record Details");

    // current search text
    private String currentSearchFilter = "";

    // constructor
    public ArchiveView(
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService,
            DispatchService dispatchService,
            UserAdministrationService userService,
            IncidentManagementService incidentService,
            MaintenanceService maintenanceService,
            OperationalReportService reportService) {

        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;
        this.userService = userService;
        this.incidentService = incidentService;
        this.maintenanceService = maintenanceService;
        this.reportService = reportService;

        configureView();
        configureArchiveTabs();

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
        configureUserGrid();
        configureCompletedWorkGrids();
        configureReportGrid();

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
        tabSheet.add(new Tab("Archived Users"), userGrid);
        tabSheet.add(new Tab("Archived Maintenance"), maintenanceGrid);
        tabSheet.add(new Tab("Archived Incidents"), incidentGrid);
        tabSheet.add(new Tab("Archived Operational Reports"), reportGrid);

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

        add(primaryContent);
        expand(primaryContent);
    }

    // archive header
    private HorizontalLayout createHeader() {

        H2 title =
                new H2("Archived Data");

        title.getStyle()
                .set("margin", "0");

        TextField searchField =
                FormInputs.text(new TextField(), 200);

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
                        new HorizontalLayout(searchField, new RefreshButton(this::refreshAll))
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

    private void configureCompletedWorkGrids() {
        maintenanceGrid.setSizeFull();
        maintenanceGrid.addColumn(MaintenanceRecordView::id).setHeader("Maintenance Number");
        maintenanceGrid.addColumn(record -> fleetService.getUnit(record.fleetUnitId()).unitNumber()).setHeader("Bus");
        maintenanceGrid.addColumn(MaintenanceRecordView::maintenanceType).setHeader("Type");
        maintenanceGrid.addColumn(MaintenanceRecordView::maintenanceStatus).setHeader("Status");
        maintenanceGrid.addColumn(MaintenanceRecordView::description).setHeader("Description").setWidth("300px");
        maintenanceGrid.addColumn(MaintenanceRecordView::completedAt).setHeader("Completed At");
        maintenanceGrid.addColumn(MaintenanceRecordView::technicianNotes).setHeader("Technician Notes").setWidth("300px");
        maintenanceGrid.addComponentColumn(record -> new Button("Details & History",
                event -> showMaintenanceDetails(record))).setHeader("Actions");
        maintenanceGrid.addItemDoubleClickListener(event -> showMaintenanceDetails(event.getItem()));
        maintenanceGrid.setItemsPageable(pageable -> maintenanceService.searchArchivedRecords(currentSearchFilter,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "completedAt", "id")))
                .getContent());

        incidentGrid.setSizeFull();
        incidentGrid.addColumn(IncidentView::id).setHeader("Incident Number");
        incidentGrid.addColumn(IncidentView::title).setHeader("Title").setWidth("260px");
        incidentGrid.addColumn(IncidentView::severity).setHeader("Severity");
        incidentGrid.addColumn(IncidentView::status).setHeader("Status");
        incidentGrid.addColumn(IncidentView::reportedAt).setHeader("Reported At");
        incidentGrid.addColumn(IncidentView::resolvedAt).setHeader("Resolved At");
        incidentGrid.addColumn(IncidentView::resolutionNotes).setHeader("Resolution Notes").setWidth("300px");
        incidentGrid.addComponentColumn(incident -> new Button("Details & History",
                event -> showIncidentDetails(incident))).setHeader("Actions");
        incidentGrid.addItemDoubleClickListener(event -> showIncidentDetails(event.getItem()));
        incidentGrid.setItemsPageable(pageable -> incidentService.searchArchivedIncidents(currentSearchFilter,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "reportedAt", "id")))
                .getContent());
    }

    private void showMaintenanceDetails(MaintenanceRecordView record) {
        Dialog dialog = Dialogs.details("Archived Maintenance Details");
        Dialogs.field(dialog, "Maintenance number", record.id());
        Dialogs.field(dialog, "Bus", fleetService.getUnit(record.fleetUnitId()).unitNumber());
        Dialogs.field(dialog, "Type", record.maintenanceType());
        Dialogs.field(dialog, "Status", record.maintenanceStatus());
        Dialogs.field(dialog, "Description", record.description());
        Dialogs.field(dialog, "Completed at", record.completedAt());
        Dialogs.field(dialog, "Technician notes", record.technicianNotes());
        Grid<com.transit.arctransit.maintenance.MaintenanceHistoryView> history = DataGrids.create();
        history.addColumn(com.transit.arctransit.maintenance.MaintenanceHistoryView::recordedAt).setHeader("Time");
        history.addColumn(com.transit.arctransit.maintenance.MaintenanceHistoryView::status).setHeader("Status");
        history.addColumn(com.transit.arctransit.maintenance.MaintenanceHistoryView::recordedBy).setHeader("By");
        history.addColumn(com.transit.arctransit.maintenance.MaintenanceHistoryView::reason).setHeader("Reason").setWidth("300px");
        history.setHeight("250px");
        history.setItemsPageable(pageable -> maintenanceService.history(record.id(), pageable).getContent());
        dialog.add(new H3("History"), history);
        dialog.open();
    }

    private void showIncidentDetails(IncidentView incident) {
        Dialog dialog = Dialogs.details("Archived Incident Details");
        Dialogs.field(dialog, "Incident number", incident.id());
        Dialogs.field(dialog, "Title", incident.title());
        Dialogs.field(dialog, "Description", incident.description());
        Dialogs.field(dialog, "Severity", incident.severity());
        Dialogs.field(dialog, "Status", incident.status());
        Dialogs.field(dialog, "Reported by", incident.reportedBy());
        Dialogs.field(dialog, "Reported at", incident.reportedAt());
        Dialogs.field(dialog, "Resolved at", incident.resolvedAt());
        Dialogs.field(dialog, "Resolution notes", incident.resolutionNotes());
        Dialogs.field(dialog, "Linked maintenance number", incident.maintenanceRecordId());
        Grid<com.transit.arctransit.incident.IncidentEventView> history = DataGrids.create();
        history.addColumn(com.transit.arctransit.incident.IncidentEventView::recordedAt).setHeader("Time");
        history.addColumn(com.transit.arctransit.incident.IncidentEventView::newStatus).setHeader("Status");
        history.addColumn(com.transit.arctransit.incident.IncidentEventView::recordedBy).setHeader("By");
        history.addColumn(com.transit.arctransit.incident.IncidentEventView::reason).setHeader("Reason").setWidth("300px");
        history.setHeight("250px");
        history.setItems(incidentService.getHistory(incident.id()));
        dialog.add(new H3("History"), history);
        dialog.open();
    }

    private void configureReportGrid() {
        reportGrid.setSizeFull();
        reportGrid.addColumn(OperationalReportView::id).setHeader("Report Number");
        reportGrid.addColumn(this::reportRecordLabel).setHeader("Affected Record").setWidth("240px");
        reportGrid.addColumn(OperationalReportView::description).setHeader("Reported Information").setWidth("320px");
        reportGrid.addColumn(OperationalReportView::sourceType).setHeader("Source");
        reportGrid.addColumn(OperationalReportView::verificationStatus).setHeader("Verification");
        reportGrid.addColumn(OperationalReportView::reportedBy).setHeader("Recorded By");
        reportGrid.addColumn(OperationalReportView::archivedAt).setHeader("Archived At");
        reportGrid.addComponentColumn(report -> {
            Button restore = new Button("Restore", event -> {
                try {
                    reportService.restoreReport(report.id());
                    reportGrid.getDataProvider().refreshAll();
                    showSuccess("Operational report restored.");
                } catch (RuntimeException exception) {
                    ApplicationErrors.show(exception);
                }
            });
            Button delete = new Button("Delete Permanently", event -> openDeleteDialog("Operational Report " + report.id(), () -> {
                reportService.hardDeleteReport(report.id());
                reportGrid.getDataProvider().refreshAll();
            }));
            delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(new Button("Details & History", event -> showReportDetails(report)), restore, delete);
        }).setHeader("Actions");
        reportGrid.addItemDoubleClickListener(event -> showReportDetails(event.getItem()));
        reportGrid.setItemsPageable(pageable -> reportService.searchArchivedReports(currentSearchFilter,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "archivedAt", "id")))
                .getContent());
    }

    private String reportRecordLabel(OperationalReportView report) {
        try {
            return switch (report.entityType()) {
                case "FLEET" -> fleetService.getUnit(report.entityId()).unitNumber();
                case "DRIVER" -> {
                    var driver = driverService.getDriver(report.entityId());
                    yield driver.employeeNumber() + " - " + driver.firstName() + " " + driver.lastName();
                }
                case "ROUTE" -> {
                    var route = routeService.getRoute(report.entityId());
                    yield route.routeCode() + " - " + route.routeName();
                }
                default -> {
                    var trip = dispatchService.getAssignment(report.entityId());
                    yield "Dispatch #" + trip.id() + " - " + trip.fleetUnitNumber() + " - " + trip.routeCode();
                }
            };
        } catch (com.transit.arctransit.common.exception.ResourceNotFoundException exception) {
            return report.entityType() + " #" + report.entityId() + " (no longer available)";
        }
    }

    private void showReportDetails(OperationalReportView report) {
        Dialog dialog = Dialogs.details("Archived Operational Report Details");
        Dialogs.field(dialog, "Report number", report.id());
        Dialogs.field(dialog, "Affected record", reportRecordLabel(report));
        Dialogs.field(dialog, "Reported information", report.description());
        Dialogs.field(dialog, "Source", report.sourceType());
        Dialogs.field(dialog, "Recorded by", report.reportedBy());
        Dialogs.field(dialog, "Reported at", report.reportedAt());
        Dialogs.field(dialog, "Effective at", report.effectiveAt());
        Dialogs.field(dialog, "Verification", report.verificationStatus());
        Dialogs.field(dialog, "Reviewed by", report.verifiedBy());
        Dialogs.field(dialog, "Reviewed at", report.verifiedAt());
        Dialogs.field(dialog, "Evidence and reason", report.verificationReason());
        Dialogs.field(dialog, "Archived at", report.archivedAt());
        Grid<ReportVerificationView> history = DataGrids.create();
        history.addColumn(ReportVerificationView::recordedAt).setHeader("Time");
        history.addColumn(ReportVerificationView::newStatus).setHeader("Verification");
        history.addColumn(ReportVerificationView::recordedBy).setHeader("By");
        history.addColumn(ReportVerificationView::reason).setHeader("Reason").setWidth("300px");
        history.setHeight("250px");
        history.setItems(reportService.getVerificationHistory(report.id()));
        dialog.add(new H3("Verification History"), history);
        dialog.open();
    }

    private void configureUserGrid() {
        userGrid.setSizeFull();
        userGrid.addColumn(UserSummaryView::username).setHeader("Username");
        userGrid.addColumn(UserSummaryView::displayName).setHeader("Name");
        userGrid.addColumn(UserSummaryView::accountStatus).setHeader("Status");
        userGrid.addComponentColumn(user -> createActionButtons("User " + user.username(), () -> {
            userService.hardDeleteUser(user.username());
            userGrid.getDataProvider().refreshAll();
            detailsDialog.close();
        }, null)).setHeader("Actions");
        userGrid.setItemsPageable(pageable -> userService.searchArchivedUsers(
                new UserQuery(currentSearchFilter),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by("username"))).getContent());
        userGrid.addItemDoubleClickListener(event -> {
            var user = event.getItem();
            showDetails("Archived User", """
                        Username: %s
                        Name: %s
                        Status: %s
                        This archived account is disabled. Its identity and history are retained.
                        """.formatted(user.username(), user.displayName(), user.accountStatus()));
        });
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

        fleetGrid.addItemDoubleClickListener(event -> {
            var unit = event.getItem();
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
                                        );
        });
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

        driverGrid.addItemDoubleClickListener(event -> {
            var driver = event.getItem();
            showDetails(
                                                "Archived Driver",
                                                """
                                                Name: %s
                                                License: %s
                                                """.formatted(
                                                        driver.fullName(),
                                                        driver.licenseNumber()
                                                )
                                        );
        });
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

        routeGrid.addItemDoubleClickListener(event -> {
            var route = event.getItem();
            showDetails(
                                                "Archived Route",
                                                """
                                                Code: %s
                                                Name: %s
                                                """.formatted(
                                                        route.routeCode(),
                                                        route.routeName()
                                                )
                                        );
        });
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
                                () -> {
                                    dispatchService.hardDeleteAssignment(dispatch.id());
                                    refreshDispatches();
                                    detailsDialog.close();
                                },
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

        dispatchGrid.addItemDoubleClickListener(event -> {
            var dispatch = event.getItem();
            showDetails(
                                                "Archived Dispatch",
                                                """
                                                Record ID: %s
                                                Bus: %s
                                                Driver: %s
                                                Route: %s
                                                Status: %s
                                                """.formatted(
                                                        dispatch.id(),
                                                        dispatch.fleetUnitNumber(),
                                                        dispatch.driverName(),
                                                        dispatch.routeCode(),
                                                        dispatch.dispatchStatus()
                                                )
                                        );
        });
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
                    "Delete Permanently"
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

        ConfirmationDialog dialog =
                new ConfirmationDialog();

        dialog.setHeader(
                "Permanently Delete "
                        + entityName
                        + "?"
        );

        dialog.setText(
                "This action cannot be undone. It removes the archived record. "
                        + "Audit and retained business history remain. Linked records may prevent deletion."
        );

        dialog.setCancelable(true);

        dialog.setConfirmText(
                "Delete Permanently"
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

                        ApplicationErrors.show(exception);
                    }
                }
        );

        dialog.open();
    }

    // details panel
    private void showDetails(String title, String content) {
        detailsDialog.removeAll();
        detailsDialog.setHeaderTitle(title);
        Paragraph details = new Paragraph(content);
        details.getStyle().set("white-space", "pre-wrap").set("overflow-wrap", "anywhere");
        detailsDialog.add(details);
        detailsDialog.open();
    }

    // side panel


    // hide side panel
    private void closeDetails() {
        detailsDialog.close();
    }

    // refresh all archives
    private void refreshAll() {

        refreshFleet();
        refreshDrivers();
        refreshRoutes();
        refreshDispatches();
        userGrid.getDataProvider().refreshAll();
        maintenanceGrid.getDataProvider().refreshAll();
        incidentGrid.getDataProvider().refreshAll();
        reportGrid.getDataProvider().refreshAll();
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
