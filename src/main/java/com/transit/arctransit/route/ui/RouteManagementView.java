package com.transit.arctransit.route.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
// route module

import com.transit.arctransit.common.ui.FormInputs;

import com.transit.arctransit.route.CreateRouteCommand;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteQuery;
import com.transit.arctransit.route.RouteStopView;
import com.transit.arctransit.route.RouteSummaryView;
import com.transit.arctransit.route.RouteView;
import com.transit.arctransit.route.UpdateRouteCommand;

// common

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.common.ui.MainLayout;

// vaadin

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.transit.arctransit.common.ui.ConfirmationDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.ZoneId;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

// security

import jakarta.annotation.security.RolesAllowed;

// spring

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// java

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// route management view

@Route(
        value = "routes",
        layout = MainLayout.class
)
@PageTitle("Route Management")
@RolesAllowed({
        "SYSTEM_ADMIN",
        "OPERATIONS_STAFF"
})
public class RouteManagementView
        extends VerticalLayout {

    // constants

    private static final Logger log =
            LoggerFactory.getLogger(
                    RouteManagementView.class
            );

    private static final String SYSTEM_ADMIN_ROLE =
            "SYSTEM_ADMIN";

    private static final List<String> ROUTE_STATUSES =
            List.of(
                    "ACTIVE",
                    "INACTIVE"
            );

    // dependencies

    private final RouteManagementService routeService;

    @Value("${arc.service-zone:Asia/Manila}")
    private String serviceZone = "Asia/Manila";

    private final transient AuthenticationContext
            authenticationContext;

    // components

    private final Grid<RouteSummaryView> grid =
            DataGrids.create();


    private final Dialog detailsDialog = Dialogs.details("Route Details");


    // state

    private String currentSearchFilter = "";

    // constructor

    public RouteManagementView(
            RouteManagementService routeService,
            AuthenticationContext authenticationContext) {

        this.routeService = routeService;
        this.authenticationContext =
                authenticationContext;

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
                new H2("Route Management");

        title.getStyle()
                .set("margin", "0");

        TextField searchField =
                FormInputs.text(new TextField(), 200);

        searchField.setPlaceholder(
                "Search route code or name..."
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
                        "Create Route",
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
                RouteSummaryView::routeCode
        )
        .setKey("routeCode")
        .setHeader("Route Code")
        .setSortable(true);

        grid.addColumn(
                RouteSummaryView::routeName
        )
        .setKey("routeName")
        .setHeader("Route Name")
        .setSortable(true);

        grid.addColumn(
                RouteSummaryView::stopCount
        )
        .setKey("stopCount")
        .setHeader("# Stops")
        .setSortable(true);

        grid.addColumn(
                route ->
                        route.estimatedDurationMinutes() != null
                                ? route.estimatedDurationMinutes()
                                        + " min"
                                : "—"
        )
        .setKey("duration")
        .setHeader("Duration")
        .setSortable(true);

        grid.addColumn(
                RouteSummaryView::routeStatus
        )
        .setKey("status")
        .setHeader("Status")
        .setSortable(true);

        // lazy database-backed pagination

        grid.setItemsPageable(pageable ->
                routeService
                        .searchRoutes(
                                new RouteQuery(
                                        currentSearchFilter,
                                        null
                                ),
                                pageable
                        )
                        .getContent()
        );

        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions");

        grid.addItemDoubleClickListener(event -> showDetails(event.getItem()));
    }

    // grid action buttons

    private HorizontalLayout createActionButtons(
            RouteSummaryView route) {

        Button editButton =
                new Button(
                        "Edit",
                        event ->
                                openEditDialog(
                                        route.id()
                                )
                );

        Button statusButton =
                new Button(
                        "Status",
                        event ->
                                openStatusDialog(
                                        route.id(),
                                        route.routeStatus()
                                )
                );

        HorizontalLayout actions =
                new HorizontalLayout(
                        editButton,
                        statusButton
                );

        Button schedulesButton = new Button("Schedules", event -> openSchedulesDialog(route.id()));
        actions.add(schedulesButton);

        // archive is a system administrator operation

        if (authenticationContext.hasRole(
                SYSTEM_ADMIN_ROLE
        )) {

            Button archiveButton =
                    new Button(
                            "Archive",
                            event ->
                                    openArchiveDialog(
                                            route
                                    )
                    );

            archiveButton.addThemeVariants(
                    ButtonVariant.LUMO_ERROR
            );

            actions.add(
                    archiveButton
            );
        }

        return actions;
    }

    // side panel



    private void showDetails(
            RouteSummaryView route) {

        detailsDialog.removeAll();
        Dialogs.field(detailsDialog, "Record ID", route.id());

        detailsDialog.add(
                new Paragraph(
                        "Code: "
                                + route.routeCode()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Name: "
                                + route.routeName()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Stops: "
                                + route.stopCount()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Duration: "
                                + (
                                route.estimatedDurationMinutes()
                                        == null
                                        ? "—"
                                        : route
                                                .estimatedDurationMinutes()
                                                + " min"
                        )
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Status: "
                                + route.routeStatus()
                )
        );

        for (var version : routeService.getVersions(route.id())) {
            detailsDialog.add(new Paragraph("Version " + version.id() + ": "
                    + version.effectiveFrom() + " to "
                    + (version.effectiveTo() == null ? "open" : version.effectiveTo())
                    + (version.legacySnapshot() ? " (legacy configuration snapshot)" : "")));
        }

        detailsDialog.open();
    }

    private void closeDetails() {
        detailsDialog.close();
    }

    // create route

    private void openCreateDialog() {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Create New Route"
        );

        dialog.setWidth("600px");

        TextField routeCode =
                FormInputs.text(new TextField(
                        "Route Code"
                ), 30);

        routeCode.setRequiredIndicatorVisible(
                true
        );

        TextField routeName =
                FormInputs.text(new TextField(
                        "Route Name"
                ), 100);

        routeName.setRequiredIndicatorVisible(
                true
        );

        TextArea description =
                FormInputs.notes(new TextArea(
                        "Description"
                ));

        IntegerField duration =
                FormInputs.integer(new IntegerField(
                        "Estimated Duration (min)"
                ), 1, 2147483647);

        duration.setMin(1);

        DatePicker effectiveFrom = new DatePicker("Version Effective From");
        effectiveFrom.setValue(LocalDate.now(ZoneId.of(serviceZone)));
        effectiveFrom.setRequiredIndicatorVisible(true);

        FormLayout form =
                new FormLayout(
                        routeCode,
                        routeName,
                        description,
                        duration,
                        effectiveFrom
                );

        VerticalLayout stopsLayout =
                new VerticalLayout();

        stopsLayout.setPadding(false);

        List<StopFormEntry> stopEntries =
                new ArrayList<>();

        Button addStopButton =
                new Button(
                        "Add Stop",
                        event ->
                                addStopEntry(
                                        stopEntries,
                                        stopsLayout
                                )
                );

        addStopButton.addThemeVariants(
                ButtonVariant.LUMO_SMALL
        );

        dialog.add(
                form,
                new H2("Stops"),
                addStopButton,
                stopsLayout
        );

        Button saveButton =
                new Button(
                        "Save",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


                            try {

                                List<CreateRouteCommand.StopEntry>
                                        stops =
                                        toStopEntries(
                                                stopEntries
                                        );

                                routeService.createRoute(
                                        new CreateRouteCommand(
                                                routeCode.getValue(),
                                                routeName.getValue(),
                                                description.getValue(),
                                                duration.getValue(),
                                                stops,
                                                effectiveFrom.getValue()
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Route created successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Create route",
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

    // edit route

    private void openEditDialog(
            Long routeId) {

        final RouteView route;

        try {

            route =
                    routeService.getRoute(
                            routeId
                    );

        } catch (RuntimeException exception) {

            showError(
                    "Load route",
                    exception
            );

            return;
        }

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Publish Route Version - "
                        + route.routeCode()
        );

        dialog.setWidth("600px");

        TextField routeCode =
                FormInputs.text(new TextField(
                        "Route Code"
                ), 30);

        routeCode.setValue(
                route.routeCode()
        );

        routeCode.setRequiredIndicatorVisible(
                true
        );

        TextField routeName =
                FormInputs.text(new TextField(
                        "Route Name"
                ), 100);

        routeName.setValue(
                route.routeName()
        );

        routeName.setRequiredIndicatorVisible(
                true
        );

        TextArea description =
                FormInputs.notes(new TextArea(
                        "Description"
                ));

        description.setValue(
                route.description() == null
                        ? ""
                        : route.description()
        );

        IntegerField duration =
                FormInputs.integer(new IntegerField(
                        "Estimated Duration (min)"
                ), 1, 2147483647);

        duration.setMin(1);

        if (route.estimatedDurationMinutes() != null) {

            duration.setValue(
                    route.estimatedDurationMinutes()
            );
        }

        DatePicker effectiveFrom = new DatePicker("Version Effective From");
        var versions = routeService.getVersions(routeId);
        LocalDate earliest = versions.isEmpty() ? LocalDate.now(ZoneId.of(serviceZone))
                : versions.getFirst().effectiveFrom().plusDays(1);
        effectiveFrom.setValue(earliest.isAfter(LocalDate.now(ZoneId.of(serviceZone)))
                ? earliest : LocalDate.now(ZoneId.of(serviceZone)));
        effectiveFrom.setMin(effectiveFrom.getValue());
        DatePicker effectiveTo = new DatePicker("Version Effective To (exclusive, optional)");
        effectiveFrom.setRequiredIndicatorVisible(true);

        FormLayout form =
                new FormLayout(
                        routeCode,
                        routeName,
                        description,
                        duration,
                        effectiveFrom,
                        effectiveTo
                );

        VerticalLayout stopsLayout =
                new VerticalLayout();

        stopsLayout.setPadding(false);

        List<StopFormEntry> stopEntries =
                new ArrayList<>();

        for (RouteStopView stop :
                route.stops()) {

            addStopEntry(
                    stopEntries,
                    stopsLayout,
                    stop
            );
        }

        Button addStopButton =
                new Button(
                        "Add Stop",
                        event ->
                                addStopEntry(
                                        stopEntries,
                                        stopsLayout
                                )
                );

        addStopButton.addThemeVariants(
                ButtonVariant.LUMO_SMALL
        );

        dialog.add(
                form,
                new H2("Stops"),
                addStopButton,
                stopsLayout
        );

        Button saveButton =
                new Button(
                        "Save",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


                            try {

                                List<CreateRouteCommand.StopEntry>
                                        stops =
                                        toStopEntries(
                                                stopEntries
                                        );

                                routeService.updateRoute(
                                        new UpdateRouteCommand(
                                                routeId,
                                                routeCode.getValue(),
                                                routeName.getValue(),
                                                description.getValue(),
                                                duration.getValue(),
                                                stops,
                                                effectiveFrom.getValue(),
                                                effectiveTo.getValue()
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Route version published successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Update route",
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

    private void openSchedulesDialog(Long routeId) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Configured Route Schedules");
        dialog.setWidth("650px");
        VerticalLayout schedules = new VerticalLayout();
        schedules.setPadding(false);
        refreshSchedules(routeId, schedules);

        TextField name = FormInputs.text(new TextField("Schedule Name"), 100);
        TimePicker departure = new TimePicker("Planned Departure");
        departure.setStep(java.time.Duration.ofMinutes(1));
        DatePicker from = new DatePicker("Effective From");
        from.setValue(LocalDate.now(ZoneId.of(serviceZone)));
        DatePicker to = new DatePicker("Effective To (exclusive, optional)");
        name.setRequiredIndicatorVisible(true);
        departure.setRequiredIndicatorVisible(true);
        from.setRequiredIndicatorVisible(true);

        Button create = new Button("Create Schedule", event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

            try {
                routeService.createSchedule(new com.transit.arctransit.route.CreateRouteScheduleCommand(
                        routeId, name.getValue(), departure.getValue(), from.getValue(), to.getValue()));
                name.clear();
                departure.clear();
                refreshSchedules(routeId, schedules);
                showSuccess("Configured schedule created.");
            } catch (RuntimeException exception) {
                showError("Create schedule", exception);
            }
        });
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(schedules, new FormLayout(name, departure, from, to));
        dialog.getFooter().add(new Button("Close", event -> dialog.close()), create);
        dialog.open();
    }

    private void refreshSchedules(Long routeId, VerticalLayout content) {
        content.removeAll();
        for (var schedule : routeService.getSchedules(routeId)) {
            Paragraph details = new Paragraph(schedule.name() + " at " + schedule.plannedDeparture()
                    + ", " + schedule.effectiveFrom() + " to "
                    + (schedule.effectiveTo() == null ? "open" : schedule.effectiveTo())
                    + (schedule.active() ? " (active)" : " (inactive)"));
            Button status = new Button(schedule.active() ? "Deactivate" : "Activate", event -> {
                try {
                    routeService.changeScheduleActive(schedule.id(), !schedule.active());
                    refreshSchedules(routeId, content);
                } catch (RuntimeException exception) {
                    showError("Change schedule availability", exception);
                }
            });
            status.addThemeVariants(ButtonVariant.LUMO_SMALL);
            content.add(new HorizontalLayout(details, status));
        }
        if (content.getComponentCount() == 0) {
            content.add(new Paragraph("No configured schedules."));
        }
    }

    // route status

    private void openStatusDialog(
            Long routeId,
            String currentStatus) {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Change Route Status"
        );

        ComboBox<String> statusCombo =
                FormInputs.choice(new ComboBox<>(
                        "New Status"
                ));
        statusCombo.setRequiredIndicatorVisible(true);

        statusCombo.setItems(
                ROUTE_STATUSES
        );

        statusCombo.setValue(
                currentStatus
        );

        statusCombo.setRequiredIndicatorVisible(
                true
        );

        dialog.add(statusCombo);

        Button saveButton =
                new Button(
                        "Update Status",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


                            String selectedStatus =
                                    statusCombo.getValue();

                            if (selectedStatus == null
                                    || selectedStatus.isBlank()) {

                                showErrorNotification(
                                        "Select a route status."
                                );

                                return;
                            }

                            try {

                                routeService
                                        .changeRouteStatus(
                                                routeId,
                                                selectedStatus
                                        );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Route status updated successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Update route status",
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

    // archive route

    private void openArchiveDialog(
            RouteSummaryView route) {

        ConfirmationDialog dialog =
                new ConfirmationDialog();

        dialog.setHeader(
                "Archive Route?"
        );

        dialog.setText(
                "Archive route "
                        + route.routeCode()
                        + "? It will be removed from "
                        + "the active route list."
        );

        dialog.setCancelable(true);

        dialog.setConfirmText(
                "Archive Route"
        );

        dialog.setConfirmButtonTheme(
                "error primary"
        );

        dialog.addConfirmListener(
                event -> {

                    try {

                        routeService.archiveRoute(
                                route.id()
                        );

                        refreshGrid();

                        closeDetails();

                        showSuccess(
                                "Route archived successfully."
                        );

                    } catch (RuntimeException exception) {

                        showError(
                                "Archive route",
                                exception
                        );
                    }
                }
        );

        dialog.open();
    }

    // add new stop form entry

    private void addStopEntry(
            List<StopFormEntry> stopEntries,
            VerticalLayout stopsLayout) {

        StopFormEntry entry =
                new StopFormEntry(
                        stopEntries.size() + 1
                );

        entry.removeButton.addClickListener(
                event -> {

                    stopEntries.remove(entry);
                    stopsLayout.remove(entry.layout);
                }
        );

        stopEntries.add(entry);
        stopsLayout.add(entry.layout);
    }

    // add existing stop form entry

    private void addStopEntry(
            List<StopFormEntry> stopEntries,
            VerticalLayout stopsLayout,
            RouteStopView stop) {

        StopFormEntry entry =
                new StopFormEntry(
                        stop.stopSequence()
                );

        entry.stopName.setValue(
                stop.stopName()
        );

        entry.sequence.setValue(
                stop.stopSequence()
        );

        if (stop.estimatedArrivalMinutes() != null) {

            entry.arrivalMinutes.setValue(
                    stop.estimatedArrivalMinutes()
            );
        }

        entry.removeButton.addClickListener(
                event -> {

                    stopEntries.remove(entry);
                    stopsLayout.remove(entry.layout);
                }
        );

        stopEntries.add(entry);
        stopsLayout.add(entry.layout);
    }

    // convert UI stop forms into route commands

    private List<CreateRouteCommand.StopEntry>
    toStopEntries(
            List<StopFormEntry> stopEntries) {

        List<CreateRouteCommand.StopEntry> stops =
                new ArrayList<>();

        Set<Integer> sequences =
                new HashSet<>();

        for (StopFormEntry entry :
                stopEntries) {

            String stopName =
                    entry.stopName.getValue();

            Integer sequence =
                    entry.sequence.getValue();

            Integer arrivalMinutes =
                    entry.arrivalMinutes.getValue();

            if (stopName == null
                    || stopName.isBlank()) {

                throw new CommandValidationException(
                        "Stop name cannot be blank."
                );
            }

            if (sequence == null
                    || sequence <= 0) {

                throw new CommandValidationException(
                        "Stop sequence must be greater than zero."
                );
            }

            if (!sequences.add(sequence)) {

                throw new CommandValidationException(
                        "Duplicate stop sequence: "
                                + sequence
                );
            }

            if (arrivalMinutes != null
                    && arrivalMinutes < 0) {

                throw new CommandValidationException(
                        "Estimated arrival minutes "
                                + "cannot be negative."
                );
            }

            stops.add(
                    new CreateRouteCommand.StopEntry(
                            stopName.trim(),
                            sequence,
                            arrivalMinutes
                    )
            );
        }

        return stops;
    }

    // refresh grid

    private void refreshGrid() {

        grid.getDataProvider()
                .refreshAll();
    }

    // normalize search text

    private static String normalizeSearchFilter(
            String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
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
                        message == null || message.isBlank()
                                ? "The operation could not be completed."
                                : message,
                        5000,
                        Notification.Position.TOP_CENTER
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }

    // dynamic stop form

    private static class StopFormEntry {

        private final TextField stopName =
                FormInputs.text(new TextField("Stop Name"), 100);

        private final IntegerField sequence =
                FormInputs.integer(new IntegerField("Sequence"), 1, 2147483647);

        private final IntegerField arrivalMinutes =
                FormInputs.integer(new IntegerField("Arrival (min)"), 0, 2147483647);

        private final Button removeButton =
                new Button(
                        VaadinIcon.TRASH.create()
                );

        private final HorizontalLayout layout;

        private StopFormEntry(
                int defaultSequence) {

            sequence.setValue(
                    defaultSequence
            );

            sequence.setMin(1);

            arrivalMinutes.setMin(0);

            removeButton.addThemeVariants(
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY
            );

            removeButton.setTooltipText(
                    "Remove Stop"
            );

            layout =
                    new HorizontalLayout(
                            stopName,
                            sequence,
                            arrivalMinutes,
                            removeButton
                    );

            layout.setWidthFull();

            layout.setDefaultVerticalComponentAlignment(
                    FlexComponent.Alignment.END
            );
        }
    }
}
