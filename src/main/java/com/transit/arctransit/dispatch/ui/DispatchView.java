package com.transit.arctransit.dispatch.ui;

import com.transit.arctransit.dispatch.*;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverQuery;
import com.transit.arctransit.driver.DriverSummaryView;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.fleet.FleetUnitSummaryView;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteQuery;
import com.transit.arctransit.route.RouteSummaryView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import java.time.format.DateTimeFormatter;
import org.springframework.security.core.context.SecurityContextHolder;
import com.transit.arctransit.common.ui.MainLayout;

/**
 * Dispatch Management CRUD view accessible to all authenticated staff.
 *
 * Features:
 * - Grid showing all assignments with resolved bus/driver/route names
 * - Create assignment dialog with ComboBox dropdowns for active entities
 * - Status transition buttons (Start, Complete, Cancel) per row
 *
 * Vaadin ComboBox reference:
 * https://vaadin.com/docs/latest/components/combo-box
 * (Ctrl+F: ComboBox)
 */
@Route(value = "dispatch", layout = MainLayout.class)
@PageTitle("Dispatch & Assignment")
@PermitAll
public class DispatchView extends VerticalLayout {

    private final DispatchService dispatchService;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final Grid<DispatchAssignmentView> grid = new Grid<>();
    private final com.vaadin.flow.component.splitlayout.SplitLayout splitLayout = new com.vaadin.flow.component.splitlayout.SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final com.vaadin.flow.component.html.Div sidePanelContent = new com.vaadin.flow.component.html.Div();
    private String currentSearchFilter = "";

    public DispatchView(DispatchService dispatchService,
                        FleetManagementService fleetService,
                        DriverManagementService driverService,
                        RouteManagementService routeService) {
        this.dispatchService = dispatchService;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        setSizeFull();
        setPadding(false);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(true);

        H2 title = new H2("Dispatch & Assignment");
        title.getStyle().set("margin", "0");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
        searchField.setPlaceholder("Search driver or route...");
        searchField.setPrefixComponent(com.vaadin.flow.component.icon.VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearchFilter = e.getValue();
            refreshGrid();
        });

        Button createBtn = new Button("Create Assignment", click -> openCreateDialog());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        actionsLayout.add(searchField, createBtn);
        actionsLayout.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headerLayout.add(title, actionsLayout);

        configureGrid();
        
        VerticalLayout gridContainer = new VerticalLayout(grid);
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        
        Button closeButton = new Button(com.vaadin.flow.component.icon.VaadinIcon.CLOSE.create(), e -> hideSidePanel());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout sidePanelHeader = new HorizontalLayout(new com.vaadin.flow.component.html.H3("Assignment Details"), closeButton);
        sidePanelHeader.setWidthFull();
        sidePanelHeader.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        sidePanelHeader.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        
        sidePanel.add(sidePanelHeader, sidePanelContent);
        sidePanel.setVisible(false);
        sidePanel.setWidth("350px");
        sidePanel.getStyle().set("background-color", "#ffffff").set("border-left", "1px solid #ddd");

        splitLayout.addToPrimary(gridContainer);
        splitLayout.addToSecondary(sidePanel);
        splitLayout.setSplitterPosition(100);
        splitLayout.setSizeFull();

        add(headerLayout, splitLayout);
        expand(splitLayout);

        refreshGrid();
    }
    
    private void showDetails(DispatchAssignmentView assignment) {
        sidePanelContent.removeAll();
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Date: " + assignment.dispatchDate()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Bus: " + assignment.fleetUnitNumber()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Driver: " + assignment.driverName()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Route: " + assignment.routeCode()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Status: " + assignment.dispatchStatus()));
        sidePanel.setVisible(true);
        splitLayout.setSplitterPosition(75);
    }
    
    private void hideSidePanel() {
        sidePanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
    }

    private void configureGrid() {
        grid.addColumn(DispatchAssignmentView::dispatchDate).setHeader("Date").setSortable(true);
        grid.addColumn(DispatchAssignmentView::fleetUnitNumber).setHeader("Bus").setSortable(true);
        grid.addColumn(DispatchAssignmentView::driverName).setHeader("Driver").setSortable(true);
        grid.addColumn(DispatchAssignmentView::routeCode).setHeader("Route").setSortable(true);
        grid.addColumn(DispatchAssignmentView::dispatchStatus).setHeader("Status").setSortable(true);

        grid.addComponentColumn(assignment -> {
            HorizontalLayout actions = new HorizontalLayout();

            if ("SCHEDULED".equals(assignment.dispatchStatus())) {
                Button startBtn = new Button("Start", click -> {
                    try {
                        dispatchService.startTrip(assignment.id());
                        Notification.show("Trip started", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        refreshGrid();
                    } catch (Exception e) {
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                startBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                actions.add(startBtn);
            }

            if ("IN_PROGRESS".equals(assignment.dispatchStatus())) {
                Button completeBtn = new Button("Complete", click -> {
                    try {
                        dispatchService.completeTrip(assignment.id());
                        Notification.show("Trip completed", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        refreshGrid();
                    } catch (Exception e) {
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                completeBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                actions.add(completeBtn);
            }

            if ("SCHEDULED".equals(assignment.dispatchStatus()) ||
                    "IN_PROGRESS".equals(assignment.dispatchStatus())) {
                Button cancelBtn = new Button("Cancel", click -> {
                    try {
                        dispatchService.cancelAssignment(assignment.id());
                        Notification.show("Assignment cancelled", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        refreshGrid();
                    } catch (Exception e) {
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
                actions.add(cancelBtn);
            }

            if ("COMPLETED".equals(assignment.dispatchStatus()) || "CANCELLED".equals(assignment.dispatchStatus())) {
                boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
                if (isAdmin) {
                    Button archiveBtn = new Button("Archive", click -> {
                        try {
                            dispatchService.archiveAssignment(assignment.id());
                            Notification.show("Assignment archived", 3000, Notification.Position.TOP_CENTER)
                                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            refreshGrid();
                        } catch (Exception e) {
                            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        }
                    });
                    archiveBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    actions.add(archiveBtn);
                }
            }

            return actions;
        }).setHeader("Actions");

        grid.setWidthFull();
        grid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(this::showDetails);
        });
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Dispatch Assignment");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();

        /*
         * ComboBox dropdowns populated with active entities from each module.
         * The user selects a bus, driver, and route from the dropdowns.
         */
        ComboBox<FleetUnitSummaryView> busCombo = new ComboBox<>("Bus");
        List<FleetUnitSummaryView> buses = fleetService
                .searchUnits(new FleetUnitQuery(null, null), PageRequest.of(0, 100)).getContent();
        busCombo.setItems(buses);
        busCombo.setItemLabelGenerator(b -> b.unitNumber() + " (" + b.plateNumber() + ")");

        ComboBox<DriverSummaryView> driverCombo = new ComboBox<>("Driver");
        List<DriverSummaryView> drivers = driverService
                .searchDrivers(new DriverQuery(null, null), PageRequest.of(0, 100)).getContent();
        driverCombo.setItems(drivers);
        driverCombo.setItemLabelGenerator(d -> d.fullName() + " (" + d.employeeNumber() + ")");

        ComboBox<RouteSummaryView> routeCombo = new ComboBox<>("Route");
        List<RouteSummaryView> routes = routeService
                .searchRoutes(new RouteQuery(null, null), PageRequest.of(0, 100)).getContent();
        routeCombo.setItems(routes);
        routeCombo.setItemLabelGenerator(r -> r.routeCode() + " - " + r.routeName());

        DatePicker dispatchDate = new DatePicker("Dispatch Date");
        dispatchDate.setValue(LocalDate.now());

        ComboBox<LocalTime> departureTime = new ComboBox<>("Departure Time");
        departureTime.setItems(
                LocalTime.of(5, 0), LocalTime.of(6, 0), LocalTime.of(7, 0),
                LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(11, 0), LocalTime.of(12, 0), LocalTime.of(13, 0),
                LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0),
                LocalTime.of(17, 0), LocalTime.of(18, 0), LocalTime.of(19, 0),
                LocalTime.of(20, 0), LocalTime.of(21, 0)
        );
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm a");
        departureTime.setItemLabelGenerator(time -> time.format(timeFormatter));

        TextArea notes = new TextArea("Notes");

        form.add(busCombo, driverCombo, routeCombo, dispatchDate, departureTime, notes);
        dialog.add(form);

        Button saveBtn = new Button("Create Assignment", click -> {
            try {
                FleetUnitSummaryView selectedBus = busCombo.getValue();
                DriverSummaryView selectedDriver = driverCombo.getValue();
                RouteSummaryView selectedRoute = routeCombo.getValue();

                if (selectedBus == null || selectedDriver == null || selectedRoute == null
                        || departureTime.getValue() == null) {
                    Notification.show("Please fill all required fields", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                ZonedDateTime departure = ZonedDateTime.of(
                        dispatchDate.getValue(), departureTime.getValue(), ZoneId.systemDefault());

                dispatchService.createAssignment(new CreateDispatchCommand(
                        selectedBus.id(),
                        selectedDriver.id(),
                        selectedRoute.id(),
                        dispatchDate.getValue(),
                        departure.toInstant(),
                        null,
                        notes.getValue()
                ));
                Notification.show("Assignment created successfully", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                refreshGrid();
            } catch (Exception e) {
                Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", click -> dialog.close());
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void refreshGrid() {
        var items = dispatchService.searchAssignments(
                        new DispatchQuery(null, null), PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(a -> 
                (a.driverName() != null && a.driverName().toLowerCase().contains(q)) || 
                (a.routeCode() != null && a.routeCode().toLowerCase().contains(q))
            ).toList();
        }
        grid.setItems(items);
    }
}
