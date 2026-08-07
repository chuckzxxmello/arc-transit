package com.transit.arctransit.route.ui;

import com.transit.arctransit.route.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import com.transit.arctransit.common.ui.MainLayout;

/**
 * Route Management view for creating, viewing, and dynamic stop administration.
 *
 * Includes a dialog-based form with dynamic stop list management
 * for adding and removing ordered stops.
 */
@Route(value = "routes", layout = MainLayout.class)
@PageTitle("Route Management")
@RolesAllowed("SYSTEM_ADMIN")
public class RouteManagementView extends VerticalLayout {

    private final RouteManagementService routeService;
    private final Grid<RouteSummaryView> grid = new Grid<>();
    private final com.vaadin.flow.component.splitlayout.SplitLayout splitLayout = new com.vaadin.flow.component.splitlayout.SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final com.vaadin.flow.component.html.Div sidePanelContent = new com.vaadin.flow.component.html.Div();
    private String currentSearchFilter = "";

    public RouteManagementView(RouteManagementService routeService) {
        this.routeService = routeService;
        setSizeFull();
        setPadding(false);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(true);

        H2 title = new H2("Route Management");
        title.getStyle().set("margin", "0");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
        searchField.setPlaceholder("Search route code or name...");
        searchField.setPrefixComponent(com.vaadin.flow.component.icon.VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearchFilter = e.getValue();
            refreshGrid();
        });

        Button addButton = new Button("Create Route", click -> openCreateDialog());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        actionsLayout.add(searchField, addButton);
        actionsLayout.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headerLayout.add(title, actionsLayout);

        configureGrid();
        
        VerticalLayout gridContainer = new VerticalLayout(grid);
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        
        Button closeButton = new Button(com.vaadin.flow.component.icon.VaadinIcon.CLOSE.create(), e -> hideSidePanel());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout sidePanelHeader = new HorizontalLayout(new com.vaadin.flow.component.html.H3("Route Details"), closeButton);
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
    
    private void showDetails(RouteSummaryView route) {
        sidePanelContent.removeAll();
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Code: " + route.routeCode()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Name: " + route.routeName()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Stops: " + route.stopCount()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Status: " + route.routeStatus()));
        sidePanel.setVisible(true);
        splitLayout.setSplitterPosition(75);
    }
    
    private void hideSidePanel() {
        sidePanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
    }

    private void configureGrid() {
        grid.addColumn(RouteSummaryView::routeCode).setHeader("Route Code").setSortable(true);
        grid.addColumn(RouteSummaryView::routeName).setHeader("Route Name").setSortable(true);
        grid.addColumn(RouteSummaryView::stopCount).setHeader("# Stops").setSortable(true);
        grid.addColumn(summary -> summary.estimatedDurationMinutes() != null
                        ? summary.estimatedDurationMinutes() + " min" : "—")
                .setHeader("Duration").setSortable(true);
        grid.addColumn(RouteSummaryView::routeStatus).setHeader("Status").setSortable(true);

        grid.addComponentColumn(route -> {
            Button editBtn = new Button("Edit", click -> openEditDialog(route.id()));
            Button statusBtn = new Button("Status", click -> openStatusDialog(route.id(), route.routeStatus()));
            Button archiveBtn = new Button("Archive", click -> archiveRoute(route.id()));
            archiveBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, statusBtn, archiveBtn);
        }).setHeader("Actions");

        grid.setWidthFull();
        grid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(this::showDetails);
        });
    }

    private void openCreateDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New Route");
        dialog.setWidth("600px");

        FormLayout form = new FormLayout();
        TextField routeCode = new TextField("Route Code");
        TextField routeName = new TextField("Route Name");
        TextArea description = new TextArea("Description");
        IntegerField duration = new IntegerField("Estimated Duration (min)");
        duration.setMin(1);

        form.add(routeCode, routeName, description, duration);
        dialog.add(form);

        /*
         * Dynamic stop list: users can add stops to the route.
         * Each stop has a name, sequence number, and estimated arrival minutes.
         */
        VerticalLayout stopsLayout = new VerticalLayout();
        stopsLayout.setPadding(false);
        List<StopFormEntry> stopEntries = new ArrayList<>();

        Button addStopBtn = new Button("Add Stop");
        addStopBtn.addClickListener(click -> {
            final StopFormEntry[] entryHolder = new StopFormEntry[1];
            entryHolder[0] = new StopFormEntry(stopEntries.size() + 1, () -> {
                stopEntries.remove(entryHolder[0]);
                stopsLayout.remove(entryHolder[0].layout);
            });
            stopEntries.add(entryHolder[0]);
            stopsLayout.add(entryHolder[0].layout);
        });
        addStopBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        dialog.add(new H2("Stops"), addStopBtn, stopsLayout);

        Button saveBtn = new Button("Save", click -> {
            try {
                List<CreateRouteCommand.StopEntry> stops = stopEntries.stream()
                        .map(e -> new CreateRouteCommand.StopEntry(
                                e.stopName.getValue(),
                                e.sequence.getValue(),
                                e.arrivalMinutes.getValue()
                        ))
                        .toList();

                routeService.createRoute(new CreateRouteCommand(
                        routeCode.getValue(),
                        routeName.getValue(),
                        description.getValue(),
                        duration.getValue(),
                        stops
                ));
                Notification.show("Route created successfully", 3000, Notification.Position.TOP_CENTER)
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

    private void openEditDialog(Long routeId) {
        RouteView route = routeService.getRoute(routeId);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Route — " + route.routeCode());
        dialog.setWidth("600px");

        FormLayout form = new FormLayout();
        TextField routeCode = new TextField("Route Code");
        routeCode.setValue(route.routeCode());
        TextField routeName = new TextField("Route Name");
        routeName.setValue(route.routeName());
        TextArea description = new TextArea("Description");
        description.setValue(route.description() != null ? route.description() : "");
        IntegerField duration = new IntegerField("Estimated Duration (min)");
        duration.setMin(1);
        if (route.estimatedDurationMinutes() != null) {
            duration.setValue(route.estimatedDurationMinutes());
        }

        form.add(routeCode, routeName, description, duration);
        dialog.add(form);

        VerticalLayout stopsLayout = new VerticalLayout();
        stopsLayout.setPadding(false);
        List<StopFormEntry> stopEntries = new ArrayList<>();

        for (RouteStopView stop : route.stops()) {
            final StopFormEntry[] entryHolder = new StopFormEntry[1];
            entryHolder[0] = new StopFormEntry(stop.stopSequence(), () -> {
                stopEntries.remove(entryHolder[0]);
                stopsLayout.remove(entryHolder[0].layout);
            });
            entryHolder[0].stopName.setValue(stop.stopName());
            entryHolder[0].sequence.setValue(stop.stopSequence());
            if (stop.estimatedArrivalMinutes() != null) {
                entryHolder[0].arrivalMinutes.setValue(stop.estimatedArrivalMinutes());
            }
            stopEntries.add(entryHolder[0]);
            stopsLayout.add(entryHolder[0].layout);
        }

        Button addStopBtn = new Button("Add Stop");
        addStopBtn.addClickListener(click -> {
            final StopFormEntry[] entryHolder = new StopFormEntry[1];
            entryHolder[0] = new StopFormEntry(stopEntries.size() + 1, () -> {
                stopEntries.remove(entryHolder[0]);
                stopsLayout.remove(entryHolder[0].layout);
            });
            stopEntries.add(entryHolder[0]);
            stopsLayout.add(entryHolder[0].layout);
        });
        addStopBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);

        dialog.add(new H2("Stops"), addStopBtn, stopsLayout);

        Button saveBtn = new Button("Save", click -> {
            try {
                List<CreateRouteCommand.StopEntry> stops = stopEntries.stream()
                        .map(e -> new CreateRouteCommand.StopEntry(
                                e.stopName.getValue(),
                                e.sequence.getValue(),
                                e.arrivalMinutes.getValue()
                        ))
                        .toList();

                routeService.updateRoute(new UpdateRouteCommand(
                        routeId,
                        routeCode.getValue(),
                        routeName.getValue(),
                        description.getValue(),
                        duration.getValue(),
                        stops
                ));
                Notification.show("Route updated successfully", 3000, Notification.Position.TOP_CENTER)
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

    private void openStatusDialog(Long routeId, String currentStatus) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change Route Status");

        ComboBox<String> statusCombo = new ComboBox<>("New Status");
        statusCombo.setItems("ACTIVE", "INACTIVE");
        statusCombo.setValue(currentStatus);

        dialog.add(statusCombo);

        Button saveBtn = new Button("Update Status", click -> {
            try {
                routeService.changeRouteStatus(routeId, statusCombo.getValue());
                Notification.show("Status updated", 3000, Notification.Position.TOP_CENTER)
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

    private void archiveRoute(Long routeId) {
        try {
            routeService.archiveRoute(routeId);
            Notification.show("Route archived", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshGrid() {
        var items = routeService.searchRoutes(new RouteQuery(null, null), PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(r -> r.routeName().toLowerCase().contains(q) || r.routeCode().toLowerCase().contains(q)).toList();
        }
        grid.setItems(items);
    }

    /**
     * Helper class to manage stop form fields in the dialog.
     */
    private static class StopFormEntry {
        final TextField stopName = new TextField("Stop Name");
        final IntegerField sequence = new IntegerField("Sequence");
        final IntegerField arrivalMinutes = new IntegerField("Arrival (min)");
        final Button removeBtn = new Button(VaadinIcon.TRASH.create());
        final HorizontalLayout layout;

        StopFormEntry(int defaultSequence, Runnable onRemove) {
            sequence.setValue(defaultSequence);
            sequence.setMin(1);
            arrivalMinutes.setMin(0);
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            removeBtn.setTooltipText("Remove Stop");
            removeBtn.addClickListener(e -> onRemove.run());
            layout = new HorizontalLayout(stopName, sequence, arrivalMinutes, removeBtn);
            layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        }
    }
}
