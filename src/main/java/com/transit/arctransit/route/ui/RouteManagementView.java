package com.transit.arctransit.route.ui;

// route module

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
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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

    private final transient AuthenticationContext
            authenticationContext;

    // components

    private final Grid<RouteSummaryView> grid =
            new Grid<>();

    private final SplitLayout splitLayout =
            new SplitLayout();

    private final VerticalLayout sidePanel =
            new VerticalLayout();

    private final Div sidePanelContent =
            new Div();

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
                new H2("Route Management");

        title.getStyle()
                .set("margin", "0");

        TextField searchField =
                new TextField();

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

        grid.addSelectionListener(event ->
                event.getFirstSelectedItem()
                        .ifPresent(this::showDetails)
        );
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
                        new H3("Route Details"),
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

    private void showDetails(
            RouteSummaryView route) {

        sidePanelContent.removeAll();

        sidePanelContent.add(
                new Paragraph(
                        "Code: "
                                + route.routeCode()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Name: "
                                + route.routeName()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Stops: "
                                + route.stopCount()
                )
        );

        sidePanelContent.add(
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

        sidePanelContent.add(
                new Paragraph(
                        "Status: "
                                + route.routeStatus()
                )
        );

        sidePanel.setVisible(true);

        splitLayout.setSplitterPosition(75);
    }

    private void hideSidePanel() {

        sidePanel.setVisible(false);

        splitLayout.setSplitterPosition(100);
    }

    // create route

    private void openCreateDialog() {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Create New Route"
        );

        dialog.setWidth("600px");

        TextField routeCode =
                new TextField(
                        "Route Code"
                );

        routeCode.setRequiredIndicatorVisible(
                true
        );

        TextField routeName =
                new TextField(
                        "Route Name"
                );

        routeName.setRequiredIndicatorVisible(
                true
        );

        TextArea description =
                new TextArea(
                        "Description"
                );

        IntegerField duration =
                new IntegerField(
                        "Estimated Duration (min)"
                );

        duration.setMin(1);

        FormLayout form =
                new FormLayout(
                        routeCode,
                        routeName,
                        description,
                        duration
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
                                                stops
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
                new Dialog();

        dialog.setHeaderTitle(
                "Edit Route — "
                        + route.routeCode()
        );

        dialog.setWidth("600px");

        TextField routeCode =
                new TextField(
                        "Route Code"
                );

        routeCode.setValue(
                route.routeCode()
        );

        routeCode.setRequiredIndicatorVisible(
                true
        );

        TextField routeName =
                new TextField(
                        "Route Name"
                );

        routeName.setValue(
                route.routeName()
        );

        routeName.setRequiredIndicatorVisible(
                true
        );

        TextArea description =
                new TextArea(
                        "Description"
                );

        description.setValue(
                route.description() == null
                        ? ""
                        : route.description()
        );

        IntegerField duration =
                new IntegerField(
                        "Estimated Duration (min)"
                );

        duration.setMin(1);

        if (route.estimatedDurationMinutes() != null) {

            duration.setValue(
                    route.estimatedDurationMinutes()
            );
        }

        FormLayout form =
                new FormLayout(
                        routeCode,
                        routeName,
                        description,
                        duration
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
                                                stops
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Route updated successfully."
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

    // route status

    private void openStatusDialog(
            Long routeId,
            String currentStatus) {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Change Route Status"
        );

        ComboBox<String> statusCombo =
                new ComboBox<>(
                        "New Status"
                );

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

        ConfirmDialog dialog =
                new ConfirmDialog();

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

                        hideSidePanel();

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

        if (exception instanceof BusinessConflictException
                || exception instanceof CommandValidationException
                || exception instanceof ResourceNotFoundException) {

            log.warn(
                    "{} failed: {}",
                    operation,
                    exception.getMessage()
            );

            showErrorNotification(
                    exception.getMessage()
            );

            return;
        }

        log.error(
                "{} failed unexpectedly.",
                operation,
                exception
        );

        showErrorNotification(
                "The operation could not be completed. "
                        + "Please try again."
        );
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
                new TextField("Stop Name");

        private final IntegerField sequence =
                new IntegerField("Sequence");

        private final IntegerField arrivalMinutes =
                new IntegerField("Arrival (min)");

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