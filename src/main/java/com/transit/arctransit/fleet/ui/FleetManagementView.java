package com.transit.arctransit.fleet.ui;

import com.transit.arctransit.fleet.*;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.data.domain.PageRequest;
import com.transit.arctransit.common.ui.MainLayout;

/**
 * Fleet Management CRUD view accessible to all authenticated staff.
 *
 * Displays a Grid of fleet units with controls to add, edit, change
 * status, and archive buses.
 *
 * Vaadin Grid component reference:
 * https://vaadin.com/docs/latest/components/grid
 * (Ctrl+F: Grid)
 *
 * Vaadin Dialog component reference:
 * https://vaadin.com/docs/latest/components/dialog
 * (Ctrl+F: Dialog)
 */
@Route(value = "fleet", layout = MainLayout.class)
@PageTitle("Fleet Management")
@PermitAll
public class FleetManagementView extends VerticalLayout {

    private final FleetManagementService fleetService;
    private final Grid<FleetUnitSummaryView> grid = new Grid<>();
    private final com.vaadin.flow.component.splitlayout.SplitLayout splitLayout = new com.vaadin.flow.component.splitlayout.SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final com.vaadin.flow.component.html.Div sidePanelContent = new com.vaadin.flow.component.html.Div();
    private String currentSearchFilter = "";

    public FleetManagementView(FleetManagementService fleetService) {
        this.fleetService = fleetService;
        setSizeFull();
        setPadding(false);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(true);

        H2 title = new H2("Fleet Management");
        title.getStyle().set("margin", "0");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
        searchField.setPlaceholder("Search unit or plate...");
        searchField.setPrefixComponent(com.vaadin.flow.component.icon.VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearchFilter = e.getValue();
            refreshGrid();
        });

        Button addButton = new Button("Add Bus", click -> openCreateDialog());
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
        HorizontalLayout sidePanelHeader = new HorizontalLayout(new com.vaadin.flow.component.html.H3("Bus Details"), closeButton);
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
    
    private void showDetails(FleetUnitSummaryView unit) {
        sidePanelContent.removeAll();
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Unit Number: " + unit.unitNumber()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Plate Number: " + unit.plateNumber()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Capacity: " + unit.capacity()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Status: " + unit.operationalStatus()));
        sidePanel.setVisible(true);
        splitLayout.setSplitterPosition(75);
    }
    
    private void hideSidePanel() {
        sidePanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
    }

    private void configureGrid() {
        grid.addColumn(FleetUnitSummaryView::unitNumber).setHeader("Unit Number").setSortable(true);
        grid.addColumn(FleetUnitSummaryView::plateNumber).setHeader("Plate Number").setSortable(true);
        grid.addColumn(FleetUnitSummaryView::capacity).setHeader("Capacity").setSortable(true);
        grid.addColumn(FleetUnitSummaryView::operationalStatus).setHeader("Status").setSortable(true);

        grid.addComponentColumn(unit -> {
            Button editBtn = new Button("Edit", click -> openEditDialog(unit.id()));
            Button statusBtn = new Button("Status", click -> openStatusDialog(unit.id(), unit.operationalStatus()));
            Button archiveBtn = new Button("Archive", click -> archiveUnit(unit.id()));
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
        dialog.setHeaderTitle("Register New Bus");

        FormLayout form = new FormLayout();
        TextField unitNumber = new TextField("Unit Number");
        TextField plateNumber = new TextField("Plate Number");
        IntegerField capacity = new IntegerField("Capacity");
        capacity.setMin(1);
        capacity.setValue(40);

        form.add(unitNumber, plateNumber, capacity);
        dialog.add(form);

        Button saveBtn = new Button("Save", click -> {
            try {
                fleetService.createUnit(new CreateFleetUnitCommand(
                        unitNumber.getValue(),
                        plateNumber.getValue(),
                        capacity.getValue().shortValue()
                ));
                Notification.show("Bus registered successfully", 3000, Notification.Position.TOP_CENTER)
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

    private void openEditDialog(Long unitId) {
        FleetUnitView unit = fleetService.getUnit(unitId);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Bus — " + unit.unitNumber());

        FormLayout form = new FormLayout();
        TextField unitNumber = new TextField("Unit Number");
        unitNumber.setValue(unit.unitNumber());
        TextField plateNumber = new TextField("Plate Number");
        plateNumber.setValue(unit.plateNumber());
        IntegerField capacity = new IntegerField("Capacity");
        capacity.setMin(1);
        capacity.setValue((int) unit.capacity());

        form.add(unitNumber, plateNumber, capacity);
        dialog.add(form);

        Button saveBtn = new Button("Save", click -> {
            try {
                fleetService.updateUnit(new UpdateFleetUnitCommand(
                        unitId,
                        unitNumber.getValue(),
                        plateNumber.getValue(),
                        capacity.getValue().shortValue()
                ));
                Notification.show("Bus updated successfully", 3000, Notification.Position.TOP_CENTER)
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

    private void openStatusDialog(Long unitId, String currentStatus) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change Operational Status");

        ComboBox<String> statusCombo = new ComboBox<>("New Status");
        statusCombo.setItems("ACTIVE", "INACTIVE", "UNDER_MAINTENANCE", "OUT_OF_SERVICE");
        statusCombo.setValue(currentStatus);

        dialog.add(statusCombo);

        Button saveBtn = new Button("Update Status", click -> {
            try {
                fleetService.changeStatus(unitId, statusCombo.getValue());
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

    private void archiveUnit(Long unitId) {
        try {
            fleetService.archiveUnit(unitId);
            Notification.show("Bus archived", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshGrid() {
        var items = fleetService.searchUnits(new FleetUnitQuery(null, null), PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(u -> u.unitNumber().toLowerCase().contains(q) || u.plateNumber().toLowerCase().contains(q)).toList();
        }
        grid.setItems(items);
    }
}
