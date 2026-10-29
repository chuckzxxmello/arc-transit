package com.transit.arctransit.dispatch.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
// dispatch module

import com.transit.arctransit.common.ui.FormInputs;

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
import com.transit.arctransit.common.ui.ConfirmationDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.IntegerField;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${arc.service-zone:Asia/Manila}")
    private String serviceZone = "Asia/Manila";

    // view components

    private final Grid<DispatchAssignmentView> grid =
            DataGrids.create();


    private final Dialog detailsDialog = Dialogs.details("Assignment Details");


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

        add(gridContainer);
        expand(gridContainer);
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
                FormInputs.text(new TextField(), 200);

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
                        createButton,
                        new RefreshButton(this::refreshGrid)
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
                assignment -> formatTime(assignment.scheduledDeparture())
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

        grid.addItemDoubleClickListener(event -> showDetails(event.getItem()));
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

        detailsDialog.removeAll();
        Dialogs.field(detailsDialog, "Record ID", assignment.id());

        detailsDialog.add(
                new Paragraph(
                        "Date: "
                                + assignment.dispatchDate()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Bus: "
                                + assignment.fleetUnitNumber()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Driver: "
                                + assignment.driverName()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Route: "
                                + assignment.routeCode()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Departure: "
                                + formatTime(assignment.scheduledDeparture())
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Status: "
                                + assignment.dispatchStatus()
                )
        );

        detailsDialog.add(new Paragraph("Planned arrival: " + formatTime(assignment.scheduledArrival())));
        detailsDialog.add(new Paragraph("Reported departure: " + formatTime(assignment.actualDeparture())));
        detailsDialog.add(new Paragraph("Reported arrival: " + formatTime(assignment.actualArrival())));
        if (assignment.scheduleId() != null) {
            detailsDialog.add(new Paragraph("Configured schedule: " + assignment.scheduleId()));
        }
        detailsDialog.add(new Paragraph("Route version: " + assignment.routeVersionId()
                + (assignment.legacyRouteSnapshot() ? " (legacy configuration snapshot)" : "")));
        for (var event : dispatchService.getHistory(assignment.id())) {
            detailsDialog.add(new Paragraph(event.newStatus() + " recorded "
                    + event.recordedAt().atZone(ZoneId.of(serviceZone)) + " by " + event.actor()));
        }

        detailsDialog.open();
    }

    // side panel



    private void closeDetails() {
        detailsDialog.close();
    }

    // create assignment

    private void openCreateDialog() {

        Dialog dialog =
                Dialogs.create();

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
        dispatchDate.setRequiredIndicatorVisible(true);

        dispatchDate.setValue(
                LocalDate.now(ZoneId.of(serviceZone))
        );

        ComboBox<LocalTime> departureTime =
                FormInputs.choice(new ComboBox<>(
                        "Departure Time"
                ));
        departureTime.setRequiredIndicatorVisible(true);

        departureTime.setItems(
                DEPARTURE_TIMES
        );

        departureTime.setItemLabelGenerator(
                TIME_FORMATTER::format
        );

        TextArea notes =
                FormInputs.notes(new TextArea("Notes"));

        ComboBox<com.transit.arctransit.route.RouteScheduleView> scheduleCombo =
                FormInputs.choice(new ComboBox<>("Configured Schedule (optional)"));
        scheduleCombo.setClearButtonVisible(true);
        scheduleCombo.setItemLabelGenerator(schedule -> schedule.name() + " " + schedule.plannedDeparture());
        Runnable reloadSchedules = () -> {
            scheduleCombo.clear();
            var selected = routeCombo.getValue();
            if (selected == null || dispatchDate.getValue() == null) {
                scheduleCombo.setItems(List.of());
                return;
            }
            scheduleCombo.setItems(routeService.getSchedules(selected.id()).stream()
                    .filter(schedule -> schedule.active() && schedule.isEffectiveOn(dispatchDate.getValue()))
                    .toList());
        };
        scheduleCombo.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                LocalTime configured = event.getValue().plannedDeparture();
                var times = new java.util.ArrayList<>(DEPARTURE_TIMES);
                if (!times.contains(configured)) {
                    times.add(configured);
                    times.sort(LocalTime::compareTo);
                }
                departureTime.setItems(times);
                departureTime.setValue(configured);
            }
        });

        IntegerField plannedDuration = FormInputs.integer(new IntegerField("Planned Duration (min)"), 1, 2147483647);
        plannedDuration.setMin(1);
        plannedDuration.setRequiredIndicatorVisible(true);
        routeCombo.addValueChangeListener(event -> {
            reloadSchedules.run();
            var selected = event.getValue();
            if (selected != null && dispatchDate.getValue() != null) {
                try {
                    plannedDuration.setValue(routeService.getEffectiveVersion(selected.id(),
                            dispatchDate.getValue()).estimatedDurationMinutes());
                } catch (RuntimeException exception) {
                    plannedDuration.clear();
                    showError("Load route version", exception);
                }
            }
        });

        dispatchDate.addValueChangeListener(event -> {
            reloadSchedules.run();
            var selected = routeCombo.getValue();
            if (selected != null && event.getValue() != null) {
                try {
                    plannedDuration.setValue(routeService.getEffectiveVersion(selected.id(),
                            event.getValue()).estimatedDurationMinutes());
                } catch (RuntimeException exception) {
                    plannedDuration.clear();
                    showError("Load route version", exception);
                }
            }
        });

        FormLayout form =
                new FormLayout(
                        busCombo,
                        driverCombo,
                        routeCombo,
                        dispatchDate,
                        scheduleCombo,
                        departureTime,
                        plannedDuration,
                        notes
                );

        dialog.add(form);

        Button saveButton =
                new Button(
                        "Create Assignment",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


                            try {

                                CreateDispatchCommand command =
                                        buildCreateCommand(
                                                busCombo,
                                                driverCombo,
                                                routeCombo,
                                                dispatchDate,
                                                departureTime,
                                                plannedDuration,
                                                scheduleCombo,
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
                FormInputs.choice(new ComboBox<>("Bus"));

        combo.setItems(query -> fleetService.searchUnits(new FleetUnitQuery(query.getFilter().orElse(""), "ACTIVE"),
                PageRequest.of(query.getPage(), query.getPageSize(),
                        org.springframework.data.domain.Sort.by("id"))).stream());
        combo.setPlaceholder("Type to search");

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
                FormInputs.choice(new ComboBox<>("Driver"));

        combo.setItems(query -> driverService.searchDispatchCandidates(query.getFilter().orElse(""),
                PageRequest.of(query.getPage(), query.getPageSize(),
                        org.springframework.data.domain.Sort.by("id"))).stream());
        combo.setPlaceholder("Type to search");

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
                FormInputs.choice(new ComboBox<>("Route"));

        combo.setItems(query -> routeService.searchRoutes(new RouteQuery(query.getFilter().orElse(""), "ACTIVE"),
                PageRequest.of(query.getPage(), query.getPageSize(),
                        org.springframework.data.domain.Sort.by("id"))).stream());
        combo.setPlaceholder("Type to search");

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
            IntegerField plannedDuration,
            ComboBox<com.transit.arctransit.route.RouteScheduleView> scheduleCombo,
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
                || selectedTime == null
                || plannedDuration.getValue() == null || plannedDuration.getValue() <= 0) {

            throw new com.transit.arctransit.common.exception.CommandValidationException(
                    "Please fill all required fields."
            );
        }

        ZonedDateTime departure =
                ZonedDateTime.of(
                        selectedDate,
                        selectedTime,
                        ZoneId.of(serviceZone)
                );

        return new CreateDispatchCommand(
                selectedBus.id(),
                selectedDriver.id(),
                selectedRoute.id(),
                selectedDate,
                departure.toInstant(),
                departure.toInstant().plusSeconds(plannedDuration.getValue() * 60L),
                normalizeOptional(
                        notes.getValue()
                ),
                scheduleCombo.getValue() == null ? null : scheduleCombo.getValue().id()
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

        ConfirmationDialog dialog =
                new ConfirmationDialog();

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

                        closeDetails();

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

    private String formatTime(String value) {
        return value == null ? "Not recorded"
                : Instant.parse(value).atZone(ZoneId.of(serviceZone))
                        .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm z"));
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
        com.transit.arctransit.common.ui.ApplicationErrors.show(exception);
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
