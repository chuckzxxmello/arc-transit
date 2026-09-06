package com.transit.arctransit.dispatch.ui;

// dispatch module

import com.transit.arctransit.dispatch.CreateDispatchCommand;
import com.transit.arctransit.dispatch.DispatchAssignmentView;
import com.transit.arctransit.dispatch.DispatchQuery;
import com.transit.arctransit.dispatch.DispatchService;

// driver module

import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverQuery;
import com.transit.arctransit.driver.DriverSummaryView;

// fleet module

import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.fleet.FleetUnitSummaryView;

// route module

import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteQuery;
import com.transit.arctransit.route.RouteSummaryView;

// common UI

import com.transit.arctransit.common.ui.MainLayout;

// vaadin

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

// security

import jakarta.annotation.security.RolesAllowed;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// java

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// dispatch management view

@Route(
        value = "dispatch",
        layout = MainLayout.class
)
@PageTitle("Dispatch & Assignment")
@RolesAllowed({
        "SYSTEM_ADMIN",
        "OPERATIONS_STAFF"
})
public class DispatchView extends VerticalLayout {

    // constants

    private static final String SYSTEM_ADMIN_ROLE =
            "ROLE_SYSTEM_ADMIN";

    private static final List<LocalTime> DEPARTURE_TIMES =
            List.of(
                    LocalTime.of(5, 0),
                    LocalTime.of(6, 0),
                    LocalTime.of(7, 0),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalTime.of(10, 0),
                    LocalTime.of(11, 0),
                    LocalTime.of(12, 0),
                    LocalTime.of(13, 0),
                    LocalTime.of(14, 0),
                    LocalTime.of(15, 0),
                    LocalTime.of(16, 0),
                    LocalTime.of(17, 0),
                    LocalTime.of(18, 0),
                    LocalTime.of(19, 0),
                    LocalTime.of(20, 0),
                    LocalTime.of(21, 0)
            );

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    // dependencies

    private final DispatchService dispatchService;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;

    // view components

    private final Grid<DispatchAssignmentView> grid =
            new Grid<>();

    private final SplitLayout splitLayout =
            new SplitLayout();

    private final VerticalLayout sidePanel =
            new VerticalLayout();

    private final Div sidePanelContent =
            new Div();

    // view state

    private String currentSearchFilter = "";

    // constructor

    public DispatchView(
            DispatchService dispatchService,
            FleetManagementService fleetService,
            DriverManagementService driverService,
            RouteManagementService routeService) {

        this.dispatchService = dispatchService;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;

        configureView();
        configureSidePanel();
        configureGrid();

        HorizontalLayout header =
                createHeader();

        VerticalLayout gridContainer =
                new VerticalLayout(
                        header,
                        grid
                );

        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);

        splitLayout.addToPrimary(
                gridContainer
        );

        splitLayout.addToSecondary(
                sidePanel
        );

        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();

        add(splitLayout);

        expand(splitLayout);
    }

    // view configuration

    private void configureView() {

        setSizeFull();
        setPadding(false);
        setSpacing(false);
    }

    // header

    private HorizontalLayout createHeader() {

        HorizontalLayout header =
                new HorizontalLayout();

        header.setWidthFull();
        header.setPadding(true);

        header.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        header.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        H2 title =
                new H2("Dispatch & Assignment");

        title.getStyle()
                .set("margin", "0");

        TextField searchField =
                new TextField();

        searchField.setPlaceholder(
                "Search driver or route..."
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

            refreshGrid();
        });

        Button createButton =
                new Button(
                        "Create Assignment",
                        event ->
                                openCreateDialog()
                );

        createButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        HorizontalLayout actions =
                new HorizontalLayout(
                        searchField,
                        createButton
                );

        actions.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        header.add(
                title,
                actions
        );

        return header;
    }

    // grid

    private void configureGrid() {

        grid.setSizeFull();

        grid.addColumn(
                DispatchAssignmentView::dispatchDate
        )
        .setHeader("Date")
        .setSortable(true);

        grid.addColumn(
                DispatchAssignmentView::fleetUnitNumber
        )
        .setHeader("Bus")
        .setSortable(true);

        grid.addColumn(
                DispatchAssignmentView::driverName
        )
        .setHeader("Driver")
        .setSortable(true);

        grid.addColumn(
                DispatchAssignmentView::routeCode
        )
        .setHeader("Route")
        .setSortable(true);

        grid.addColumn(
                DispatchAssignmentView::scheduledDeparture
        )
        .setHeader("Departure")
        .setSortable(true);

        grid.addColumn(
                DispatchAssignmentView::dispatchStatus
        )
        .setHeader("Status")
        .setSortable(true);

        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions");

        // preserve database-backed pagination

        grid.setItemsPageable(pageable ->
                filterAssignments(
                        dispatchService
                                .searchAssignments(
                                        new DispatchQuery(
                                                null,
                                                null
                                        ),
                                        pageable
                                )
                                .getContent()
                )
        );

        grid.addSelectionListener(event ->
                event.getFirstSelectedItem()
                        .ifPresent(this::showDetails)
        );
    }

    // action buttons

    private HorizontalLayout createActionButtons(
            DispatchAssignmentView assignment) {

        HorizontalLayout actions =
                new HorizontalLayout();

        String status =
                assignment.dispatchStatus();

        if ("SCHEDULED".equals(status)) {

            Button startButton =
                    new Button(
                            "Start",
                            event ->
                                    startTrip(
                                            assignment.id()
                                    )
                    );

            startButton.addThemeVariants(
                    ButtonVariant.LUMO_SUCCESS
            );

            actions.add(startButton);
        }

        if ("IN_PROGRESS".equals(status)) {

            Button completeButton =
                    new Button(
                            "Complete",
                            event ->
                                    completeTrip(
                                            assignment.id()
                                    )
                    );

            completeButton.addThemeVariants(
                    ButtonVariant.LUMO_SUCCESS
            );

            actions.add(completeButton);
        }

        if ("SCHEDULED".equals(status)
                || "IN_PROGRESS".equals(status)) {

            Button cancelButton =
                    new Button(
                            "Cancel",
                            event ->
                                    cancelAssignment(
                                            assignment.id()
                                    )
                    );

            cancelButton.addThemeVariants(
                    ButtonVariant.LUMO_ERROR
            );

            actions.add(cancelButton);
        }

        if (isTerminalStatus(status)
                && isSystemAdministrator()) {

            Button archiveButton =
                    new Button(
                            "Archive",
                            event ->
                                    openArchiveDialog(
                                            assignment
                                    )
                    );

            archiveButton.addThemeVariants(
                    ButtonVariant.LUMO_TERTIARY
            );

            actions.add(archiveButton);
        }

        return actions;
    }

    // assignment details

    private void showDetails(
            DispatchAssignmentView assignment) {

        sidePanelContent.removeAll();

        sidePanelContent.add(
                new Paragraph(
                        "Date: "
                                + assignment.dispatchDate()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Bus: "
                                + assignment.fleetUnitNumber()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Driver: "
                                + assignment.driverName()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Route: "
                                + assignment.routeCode()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Departure: "
                                + assignment.scheduledDeparture()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Status: "
                                + assignment.dispatchStatus()
                )
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
                        new H3("Assignment Details"),
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

    private void hideSidePanel() {

        sidePanel.setVisible(false);

        splitLayout.setSplitterPosition(100);
    }

    // create assignment

    private void openCreateDialog() {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Create Dispatch Assignment"
        );

        dialog.setWidth("500px");

        ComboBox<FleetUnitSummaryView> busCombo =
                createBusCombo();

        ComboBox<DriverSummaryView> driverCombo =
                createDriverCombo();

        ComboBox<RouteSummaryView> routeCombo =
                createRouteCombo();

        DatePicker dispatchDate =
                new DatePicker(
                        "Dispatch Date"
                );

        dispatchDate.setValue(
                LocalDate.now()
        );

        ComboBox<LocalTime> departureTime =
                new ComboBox<>(
                        "Departure Time"
                );

        departureTime.setItems(
                DEPARTURE_TIMES
        );

        departureTime.setItemLabelGenerator(
                TIME_FORMATTER::format
        );

        TextArea notes =
                new TextArea("Notes");

        FormLayout form =
                new FormLayout(
                        busCombo,
                        driverCombo,
                        routeCombo,
                        dispatchDate,
                        departureTime,
                        notes
                );

        dialog.add(form);

        Button saveButton =
                new Button(
                        "Create Assignment",
                        event -> {

                            try {

                                CreateDispatchCommand command =
                                        buildCreateCommand(
                                                busCombo,
                                                driverCombo,
                                                routeCombo,
                                                dispatchDate,
                                                departureTime,
                                                notes
                                        );

                                dispatchService.createAssignment(
                                        command
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Assignment created successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Create assignment",
                                        exception
                                );
                            }
                        }
                );

        saveButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        Button cancelButton =
                new Button(
                        "Cancel",
                        event ->
                                dialog.close()
                );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.open();
    }

    // bus selector

    private ComboBox<FleetUnitSummaryView>
    createBusCombo() {

        ComboBox<FleetUnitSummaryView> combo =
                new ComboBox<>("Bus");

        List<FleetUnitSummaryView> buses =
                fleetService
                        .searchUnits(
                                new FleetUnitQuery(
                                        null,
                                        null
                                ),
                                PageRequest.of(0, 100)
                        )
                        .getContent()
                        .stream()
                        .filter(unit ->
                                "ACTIVE".equals(
                                        unit.operationalStatus()
                                )
                        )
                        .toList();

        combo.setItems(buses);

        combo.setItemLabelGenerator(
                bus ->
                        bus.unitNumber()
                                + " ("
                                + bus.plateNumber()
                                + ")"
        );

        combo.setRequiredIndicatorVisible(
                true
        );

        return combo;
    }

    // driver selector

    private ComboBox<DriverSummaryView>
    createDriverCombo() {

        ComboBox<DriverSummaryView> combo =
                new ComboBox<>("Driver");

        List<DriverSummaryView> drivers =
                driverService
                        .searchDrivers(
                                new DriverQuery(
                                        null,
                                        null
                                ),
                                PageRequest.of(0, 100)
                        )
                        .getContent()
                        .stream()
                        .filter(driver ->
                                "ACTIVE".equals(
                                        driver.employmentStatus()
                                )
                        )
                        .filter(driver ->
                                !driver.licenseExpired()
                        )
                        .toList();

        combo.setItems(drivers);

        combo.setItemLabelGenerator(
                driver ->
                        driver.fullName()
                                + " ("
                                + driver.employeeNumber()
                                + ")"
        );

        combo.setRequiredIndicatorVisible(
                true
        );

        return combo;
    }

    // route selector

    private ComboBox<RouteSummaryView>
    createRouteCombo() {

        ComboBox<RouteSummaryView> combo =
                new ComboBox<>("Route");

        List<RouteSummaryView> routes =
                routeService
                        .searchRoutes(
                                new RouteQuery(
                                        null,
                                        "ACTIVE"
                                ),
                                PageRequest.of(0, 100)
                        )
                        .getContent()
                        .stream()
                        .filter(route ->
                                "ACTIVE".equals(
                                        route.routeStatus()
                                )
                        )
                        .toList();

        combo.setItems(routes);

        combo.setItemLabelGenerator(
                route ->
                        route.routeCode()
                                + " - "
                                + route.routeName()
        );

        combo.setRequiredIndicatorVisible(
                true
        );

        return combo;
    }

    // build assignment command

    private CreateDispatchCommand buildCreateCommand(
            ComboBox<FleetUnitSummaryView> busCombo,
            ComboBox<DriverSummaryView> driverCombo,
            ComboBox<RouteSummaryView> routeCombo,
            DatePicker dispatchDate,
            ComboBox<LocalTime> departureTime,
            TextArea notes) {

        FleetUnitSummaryView selectedBus =
                busCombo.getValue();

        DriverSummaryView selectedDriver =
                driverCombo.getValue();

        RouteSummaryView selectedRoute =
                routeCombo.getValue();

        LocalDate selectedDate =
                dispatchDate.getValue();

        LocalTime selectedTime =
                departureTime.getValue();

        if (selectedBus == null
                || selectedDriver == null
                || selectedRoute == null
                || selectedDate == null
                || selectedTime == null) {

            throw new IllegalArgumentException(
                    "Please fill all required fields."
            );
        }

        ZonedDateTime departure =
                ZonedDateTime.of(
                        selectedDate,
                        selectedTime,
                        ZoneId.systemDefault()
                );

        return new CreateDispatchCommand(
                selectedBus.id(),
                selectedDriver.id(),
                selectedRoute.id(),
                selectedDate,
                departure.toInstant(),
                null,
                normalizeOptional(
                        notes.getValue()
                )
        );
    }

    // start trip

    private void startTrip(Long assignmentId) {

        try {

            dispatchService.startTrip(
                    assignmentId
            );

            refreshGrid();

            showSuccess(
                    "Trip started successfully."
            );

        } catch (RuntimeException exception) {

            showError(
                    "Start trip",
                    exception
            );
        }
    }

    // complete trip

    private void completeTrip(Long assignmentId) {

        try {

            dispatchService.completeTrip(
                    assignmentId
            );

            refreshGrid();

            showSuccess(
                    "Trip completed successfully."
            );

        } catch (RuntimeException exception) {

            showError(
                    "Complete trip",
                    exception
            );
        }
    }

    // cancel assignment

    private void cancelAssignment(
            Long assignmentId) {

        try {

            dispatchService.cancelAssignment(
                    assignmentId
            );

            refreshGrid();

            showSuccess(
                    "Assignment cancelled successfully."
            );

        } catch (RuntimeException exception) {

            showError(
                    "Cancel assignment",
                    exception
            );
        }
    }

    // archive assignment

    private void openArchiveDialog(
            DispatchAssignmentView assignment) {

        ConfirmDialog dialog =
                new ConfirmDialog();

        dialog.setHeader(
                "Archive Assignment?"
        );

        dialog.setText(
                "Archive the dispatch assignment for "
                        + assignment.driverName()
                        + " on route "
                        + assignment.routeCode()
                        + "?"
        );

        dialog.setCancelable(true);

        dialog.setConfirmText(
                "Archive Assignment"
        );

        dialog.setConfirmButtonTheme(
                "error primary"
        );

        dialog.addConfirmListener(
                event -> {

                    try {

                        dispatchService.archiveAssignment(
                                assignment.id()
                        );

                        refreshGrid();

                        hideSidePanel();

                        showSuccess(
                                "Assignment archived successfully."
                        );

                    } catch (RuntimeException exception) {

                        showError(
                                "Archive assignment",
                                exception
                        );
                    }
                }
        );

        dialog.open();
    }

    // refresh grid

    private void refreshGrid() {

        grid.getDataProvider()
                .refreshAll();
    }

    // filter current page

    private List<DispatchAssignmentView>
    filterAssignments(
            List<DispatchAssignmentView> assignments) {

        if (currentSearchFilter.isBlank()) {
            return assignments;
        }

        String query =
                currentSearchFilter
                        .toLowerCase();

        return assignments.stream()
                .filter(assignment ->
                        containsIgnoreCase(
                                assignment.driverName(),
                                query
                        )
                        || containsIgnoreCase(
                                assignment.routeCode(),
                                query
                        )
                        || containsIgnoreCase(
                                assignment.fleetUnitNumber(),
                                query
                        )
                )
                .toList();
    }

    // case-insensitive text check

    private static boolean containsIgnoreCase(
            String value,
            String query) {

        return value != null
                && value
                        .toLowerCase()
                        .contains(query);
    }

    // check terminal status

    private static boolean isTerminalStatus(
            String status) {

        return "COMPLETED".equals(status)
                || "CANCELLED".equals(status);
    }

    // check system administrator

    private static boolean isSystemAdministrator() {

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
                        SYSTEM_ADMIN_ROLE.equals(
                                authority.getAuthority()
                        )
                );
    }

    // normalize search text

    private static String normalizeSearchFilter(
            String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // normalize optional text

    private static String normalizeOptional(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    // success notification

    private void showSuccess(
            String message) {

        Notification.show(
                        message,
                        3000,
                        Notification.Position.TOP_CENTER
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_SUCCESS
                );
    }

    // error handling

    private void showError(
            String operation,
            RuntimeException exception) {

        showErrorNotification(
                exception.getMessage()
        );
    }

    // error notification

    private void showErrorNotification(
            String message) {

        Notification.show(
                        message == null
                                || message.isBlank()
                                ? "The operation could not be completed."
                                : message,
                        5000,
                        Notification.Position.TOP_CENTER
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }
}