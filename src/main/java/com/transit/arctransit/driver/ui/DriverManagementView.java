package com.transit.arctransit.driver.ui;

import com.transit.arctransit.driver.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;
import com.transit.arctransit.common.ui.MainLayout;

/**
 * Driver Management CRUD view for System Administrators.
 *
 * Features a Grid with a visual indicator for expired licenses
 * (red badge) to alert operations staff.
 *
 * Vaadin DatePicker reference:
 * https://vaadin.com/docs/latest/components/date-picker
 * (Ctrl+F: DatePicker)
 */
@Route(value = "drivers", layout = MainLayout.class)
@PageTitle("Driver Management")
@RolesAllowed("SYSTEM_ADMIN")
public class DriverManagementView extends VerticalLayout {

    private final DriverManagementService driverService;
    private final Grid<DriverSummaryView> grid = new Grid<>();
    private final com.vaadin.flow.component.splitlayout.SplitLayout splitLayout = new com.vaadin.flow.component.splitlayout.SplitLayout();
    private final VerticalLayout sidePanel = new VerticalLayout();
    private final com.vaadin.flow.component.html.Div sidePanelContent = new com.vaadin.flow.component.html.Div();
    private String currentSearchFilter = "";

    public DriverManagementView(DriverManagementService driverService) {
        this.driverService = driverService;
        setSizeFull();
        setPadding(false);

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(true);

        H2 title = new H2("Driver Management");
        title.getStyle().set("margin", "0");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
        searchField.setPlaceholder("Search name or license...");
        searchField.setPrefixComponent(com.vaadin.flow.component.icon.VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearchFilter = e.getValue();
            refreshGrid();
        });

        Button addButton = new Button("Register Driver", click -> openCreateDialog());
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
        HorizontalLayout sidePanelHeader = new HorizontalLayout(new com.vaadin.flow.component.html.H3("Driver Details"), closeButton);
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
    
    private void showDetails(DriverSummaryView driver) {
        sidePanelContent.removeAll();
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Employee No: " + driver.employeeNumber()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Name: " + driver.fullName()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("License: " + driver.licenseNumber()));
        sidePanelContent.add(new com.vaadin.flow.component.html.Paragraph("Status: " + driver.employmentStatus()));
        sidePanel.setVisible(true);
        splitLayout.setSplitterPosition(75);
    }
    
    private void hideSidePanel() {
        sidePanel.setVisible(false);
        splitLayout.setSplitterPosition(100);
    }

    private void configureGrid() {
        grid.addColumn(DriverSummaryView::employeeNumber).setHeader("Employee #").setSortable(true);
        grid.addColumn(DriverSummaryView::fullName).setHeader("Name").setSortable(true);
        grid.addColumn(DriverSummaryView::licenseNumber).setHeader("License #").setSortable(true);

        /*
         * License expiry column with visual expired indicator.
         * Shows a red "EXPIRED" badge when the license is past its expiry date.
         */
        grid.addComponentColumn(driver -> {
            Span expiry = new Span(driver.licenseExpiryDate());
            if (driver.licenseExpired()) {
                Span badge = new Span(" EXPIRED");
                badge.getElement().getThemeList().add("badge error");
                return new HorizontalLayout(expiry, badge);
            }
            return expiry;
        }).setHeader("License Expiry").setSortable(true);

        grid.addColumn(DriverSummaryView::employmentStatus).setHeader("Status").setSortable(true);

        grid.addComponentColumn(driver -> {
            Button editBtn = new Button("Edit", click -> openEditDialog(driver.id()));
            Button statusBtn = new Button("Status", click -> openStatusDialog(driver.id(), driver.employmentStatus()));
            Button archiveBtn = new Button("Archive", click -> archiveDriver(driver.id()));
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
        dialog.setHeaderTitle("Register New Driver");

        FormLayout form = new FormLayout();
        TextField empNumber = new TextField("Employee Number");
        TextField firstName = new TextField("First Name");
        TextField lastName = new TextField("Last Name");
        TextField licenseNumber = new TextField("License Number");
        ComboBox<String> licenseType = new ComboBox<>("License Type");
        licenseType.setItems("PROFESSIONAL", "NON_PROFESSIONAL");
        licenseType.setValue("PROFESSIONAL");
        DatePicker expiryDate = new DatePicker("License Expiry Date");
        TextField contactNumber = new TextField("Contact Number");

        form.add(empNumber, firstName, lastName, licenseNumber, licenseType, expiryDate, contactNumber);
        dialog.add(form);

        Button saveBtn = new Button("Save", click -> {
            try {
                driverService.registerDriver(new CreateDriverCommand(
                        empNumber.getValue(),
                        firstName.getValue(),
                        lastName.getValue(),
                        licenseNumber.getValue(),
                        licenseType.getValue(),
                        expiryDate.getValue(),
                        contactNumber.getValue()
                ));
                Notification.show("Driver registered successfully", 3000, Notification.Position.TOP_CENTER)
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

    private void openEditDialog(Long driverId) {
        DriverView driver = driverService.getDriver(driverId);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Driver — " + driver.firstName() + " " + driver.lastName());

        FormLayout form = new FormLayout();
        TextField empNumber = new TextField("Employee Number");
        empNumber.setValue(driver.employeeNumber());
        TextField firstName = new TextField("First Name");
        firstName.setValue(driver.firstName());
        TextField lastName = new TextField("Last Name");
        lastName.setValue(driver.lastName());
        TextField licenseNumber = new TextField("License Number");
        licenseNumber.setValue(driver.licenseNumber());
        ComboBox<String> licenseType = new ComboBox<>("License Type");
        licenseType.setItems("PROFESSIONAL", "NON_PROFESSIONAL");
        licenseType.setValue(driver.licenseType());
        DatePicker expiryDate = new DatePicker("License Expiry Date");
        expiryDate.setValue(java.time.LocalDate.parse(driver.licenseExpiryDate()));
        TextField contactNumber = new TextField("Contact Number");
        contactNumber.setValue(driver.contactNumber() != null ? driver.contactNumber() : "");

        form.add(empNumber, firstName, lastName, licenseNumber, licenseType, expiryDate, contactNumber);
        dialog.add(form);

        Button saveBtn = new Button("Save", click -> {
            try {
                driverService.updateDriver(new UpdateDriverCommand(
                        driverId,
                        empNumber.getValue(),
                        firstName.getValue(),
                        lastName.getValue(),
                        licenseNumber.getValue(),
                        licenseType.getValue(),
                        expiryDate.getValue(),
                        contactNumber.getValue()
                ));
                Notification.show("Driver updated successfully", 3000, Notification.Position.TOP_CENTER)
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

    private void openStatusDialog(Long driverId, String currentStatus) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change Employment Status");

        ComboBox<String> statusCombo = new ComboBox<>("New Status");
        statusCombo.setItems("ACTIVE", "INACTIVE", "SUSPENDED", "TERMINATED");
        statusCombo.setValue(currentStatus);

        dialog.add(statusCombo);

        Button saveBtn = new Button("Update Status", click -> {
            try {
                driverService.changeEmploymentStatus(driverId, statusCombo.getValue());
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

    private void archiveDriver(Long driverId) {
        try {
            driverService.archiveDriver(driverId);
            Notification.show("Driver archived", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshGrid() {
        var items = driverService.searchDrivers(new DriverQuery(null, null), PageRequest.of(0, 100)).getContent();
        if (currentSearchFilter != null && !currentSearchFilter.isEmpty()) {
            String q = currentSearchFilter.toLowerCase();
            items = items.stream().filter(d -> d.fullName().toLowerCase().contains(q) || d.licenseNumber().toLowerCase().contains(q)).toList();
        }
        grid.setItems(items);
    }
}
