package com.transit.arctransit.driver.ui;

// driver module

import com.transit.arctransit.driver.CreateDriverCommand;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverQuery;
import com.transit.arctransit.driver.DriverSummaryView;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.driver.UpdateDriverCommand;

// common UI

import com.transit.arctransit.common.ui.MainLayout;

// vaadin

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

// security

import jakarta.annotation.security.RolesAllowed;

// spring

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.PageRequest;

// java

import java.time.LocalDate;
import java.util.List;

/**
 * Driver Management dashboard.
 *
 * <p>System administrators and operations staff can manage
 * normal driver operations. Archiving remains restricted by
 * the application service.</p>
 */
@Route(
        value = "drivers",
        layout = MainLayout.class
)
@PageTitle("Driver Management")
@RolesAllowed({
        "SYSTEM_ADMIN",
        "OPERATIONS_STAFF"
})
public class DriverManagementView extends VerticalLayout {

    // constants

    private static final Logger log =
            LoggerFactory.getLogger(
                    DriverManagementView.class
            );

    private static final List<String> LICENSE_TYPES =
            List.of(
                    "PROFESSIONAL",
                    "NON_PROFESSIONAL"
            );

    private static final List<String> EMPLOYMENT_STATUSES =
            List.of(
                    "ACTIVE",
                    "INACTIVE",
                    "SUSPENDED",
                    "TERMINATED"
            );

    // dependencies

    private final DriverManagementService driverService;

    private final Grid<DriverSummaryView> grid =
            new Grid<>();

    private final SplitLayout splitLayout =
            new SplitLayout();

    private final VerticalLayout sidePanel =
            new VerticalLayout();

    private final Div sidePanelContent =
            new Div();
    
    private String currentSearchFilter = "";

    // constructor

    public DriverManagementView(
            DriverManagementService driverService) {

        this.driverService = driverService;

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

    // view

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
                new H2("Driver Management");

        title.getStyle().set("margin", "0");

        TextField searchField =
                new TextField();

        searchField.setPlaceholder(
                "Search name or license..."
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

        Button addButton =
                new Button(
                        "Register Driver",
                        event -> openCreateDialog()
                );

        addButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        HorizontalLayout actions =
                new HorizontalLayout(
                        searchField,
                        addButton
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
                DriverSummaryView::employeeNumber
        )
        .setHeader("Employee #")
        .setSortable(true);

        grid.addColumn(
                DriverSummaryView::fullName
        )
        .setHeader("Name")
        .setSortable(true);

        grid.addColumn(
                DriverSummaryView::licenseNumber
        )
        .setHeader("License #")
        .setSortable(true);

        grid.addComponentColumn(
                this::createLicenseExpiryComponent
        )
        .setHeader("License Expiry")
        .setSortable(true);

        grid.addColumn(
                DriverSummaryView::employmentStatus
        )
        .setHeader("Status")
        .setSortable(true);

        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions");

        grid.addSelectionListener(event ->
                event.getFirstSelectedItem()
                        .ifPresent(this::showDetails)
        );
    }

    // license expiry display

    private HorizontalLayout createLicenseExpiryComponent(
            DriverSummaryView driver) {

        Span expiry =
                new Span(
                        driver.licenseExpiryDate()
                );

        HorizontalLayout layout =
                new HorizontalLayout(expiry);

        if (driver.licenseExpired()) {

            Span badge =
                    new Span("EXPIRED");

            badge.getElement()
                    .getThemeList()
                    .add("badge error");

            layout.add(badge);
        }

        return layout;
    }

    // grid actions

    private HorizontalLayout createActionButtons(
            DriverSummaryView driver) {

        Button editButton =
                new Button(
                        "Edit",
                        event ->
                                openEditDialog(
                                        driver.id()
                                )
                );

        Button statusButton =
                new Button(
                        "Status",
                        event ->
                                openStatusDialog(
                                        driver.id(),
                                        driver.employmentStatus()
                                )
                );

        Button archiveButton =
                new Button(
                        "Archive",
                        event ->
                                openArchiveDialog(
                                        driver
                                )
                );

        archiveButton.addThemeVariants(
                ButtonVariant.LUMO_ERROR
        );

        return new HorizontalLayout(
                editButton,
                statusButton,
                archiveButton
        );
    }

    // details

    private void showDetails(
            DriverSummaryView driver) {

        sidePanelContent.removeAll();

        sidePanelContent.add(
                new Paragraph(
                        "Employee No: "
                                + driver.employeeNumber()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Name: "
                                + driver.fullName()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "License: "
                                + driver.licenseNumber()
                )
        );

        sidePanelContent.add(
                new Paragraph(
                        "Status: "
                                + driver.employmentStatus()
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
                        new H3("Driver Details"),
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

    // create driver

    private void openCreateDialog() {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Register New Driver"
        );

        TextField employeeNumber =
                new TextField(
                        "Employee Number"
                );

        TextField firstName =
                new TextField("First Name");

        TextField lastName =
                new TextField("Last Name");

        TextField licenseNumber =
                new TextField("License Number");

        ComboBox<String> licenseType =
                new ComboBox<>("License Type");

        licenseType.setItems(
                LICENSE_TYPES
        );

        licenseType.setValue(
                "PROFESSIONAL"
        );

        DatePicker expiryDate =
                new DatePicker(
                        "License Expiry Date"
                );

        TextField contactNumber =
                new TextField(
                        "Contact Number"
                );

        FormLayout form =
                new FormLayout(
                        employeeNumber,
                        firstName,
                        lastName,
                        licenseNumber,
                        licenseType,
                        expiryDate,
                        contactNumber
                );

        dialog.add(form);

        Button saveButton =
                new Button(
                        "Save",
                        event -> {

                            try {

                                driverService.registerDriver(
                                        new CreateDriverCommand(
                                                employeeNumber.getValue(),
                                                firstName.getValue(),
                                                lastName.getValue(),
                                                licenseNumber.getValue(),
                                                licenseType.getValue(),
                                                expiryDate.getValue(),
                                                contactNumber.getValue()
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Driver registered successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Register driver",
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
                        event -> dialog.close()
                );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.open();
    }

    // edit driver

    private void openEditDialog(
            Long driverId) {

        final DriverView driver;

        try {

            driver =
                    driverService.getDriver(
                            driverId
                    );

        } catch (RuntimeException exception) {

            showError(
                    "Load driver",
                    exception
            );

            return;
        }

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Edit Driver — "
                        + driver.firstName()
                        + " "
                        + driver.lastName()
        );

        TextField employeeNumber =
                new TextField(
                        "Employee Number"
                );

        employeeNumber.setValue(
                driver.employeeNumber()
        );

        TextField firstName =
                new TextField("First Name");

        firstName.setValue(
                driver.firstName()
        );

        TextField lastName =
                new TextField("Last Name");

        lastName.setValue(
                driver.lastName()
        );

        TextField licenseNumber =
                new TextField(
                        "License Number"
                );

        licenseNumber.setValue(
                driver.licenseNumber()
        );

        ComboBox<String> licenseType =
                new ComboBox<>("License Type");

        licenseType.setItems(
                LICENSE_TYPES
        );

        licenseType.setValue(
                driver.licenseType()
        );

        DatePicker expiryDate =
                new DatePicker(
                        "License Expiry Date"
                );

        expiryDate.setValue(
                LocalDate.parse(
                        driver.licenseExpiryDate()
                )
        );

        TextField contactNumber =
                new TextField(
                        "Contact Number"
                );

        contactNumber.setValue(
                driver.contactNumber() == null
                        ? ""
                        : driver.contactNumber()
        );

        FormLayout form =
                new FormLayout(
                        employeeNumber,
                        firstName,
                        lastName,
                        licenseNumber,
                        licenseType,
                        expiryDate,
                        contactNumber
                );

        dialog.add(form);

        Button saveButton =
                new Button(
                        "Save",
                        event -> {

                            try {

                                driverService.updateDriver(
                                        new UpdateDriverCommand(
                                                driverId,
                                                employeeNumber.getValue(),
                                                firstName.getValue(),
                                                lastName.getValue(),
                                                licenseNumber.getValue(),
                                                licenseType.getValue(),
                                                expiryDate.getValue(),
                                                contactNumber.getValue()
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Driver updated successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Update driver",
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
                        event -> dialog.close()
                );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.open();
    }

    // employment status

    private void openStatusDialog(
            Long driverId,
            String currentStatus) {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Change Employment Status"
        );

        ComboBox<String> statusCombo =
                new ComboBox<>("New Status");

        statusCombo.setItems(
                EMPLOYMENT_STATUSES
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

                            String status =
                                    statusCombo.getValue();

                            if (status == null
                                    || status.isBlank()) {

                                showErrorNotification(
                                        "Select an employment status."
                                );

                                return;
                            }

                            try {

                                driverService
                                        .changeEmploymentStatus(
                                                driverId,
                                                status
                                        );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Status updated successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        "Update status",
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
                        event -> dialog.close()
                );

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.open();
    }

    // archive driver

    private void openArchiveDialog(
            DriverSummaryView driver) {

        ConfirmDialog dialog =
                new ConfirmDialog();

        dialog.setHeader(
                "Archive Driver?"
        );

        dialog.setText(
                "Archive driver "
                        + driver.fullName()
                        + "? The driver will be removed "
                        + "from the active driver list."
        );

        dialog.setCancelable(true);

        dialog.setConfirmText(
                "Archive Driver"
        );

        dialog.setConfirmButtonTheme(
                "error primary"
        );

        dialog.addConfirmListener(
                event -> {

                    try {

                        driverService.archiveDriver(
                                driver.id()
                        );

                        refreshGrid();

                        hideSidePanel();

                        showSuccess(
                                "Driver archived successfully."
                        );

                    } catch (RuntimeException exception) {

                        showError(
                                "Archive driver",
                                exception
                        );
                    }
                }
        );

        dialog.open();
    }

    // refresh grid

    private void refreshGrid() {

        /*
         * Keep the existing repository/service behavior for now.
         *
         * The DriverQuery search implementation will be moved into
         * the repository in a later coordinated cleanup.
         */
        var items =
                driverService
                        .searchDrivers(
                                new DriverQuery(
                                        null,
                                        null
                                ),
                                PageRequest.of(0, 100)
                        )
                        .getContent();

        if (!currentSearchFilter.isBlank()) {

            String query =
                    currentSearchFilter
                            .toLowerCase();

            items = items.stream()
                    .filter(driver ->
                            driver.fullName()
                                    .toLowerCase()
                                    .contains(query)
                            || driver.licenseNumber()
                                    .toLowerCase()
                                    .contains(query)
                            || driver.employeeNumber()
                                    .toLowerCase()
                                    .contains(query)
                    )
                    .toList();
        }

        grid.setItems(items);
    }

    // search normalization

    private static String normalizeSearchFilter(
            String value) {

        if (value == null) {
            return "";
        }

        return value.trim();
    }

    // notifications

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

    private void showError(
            String operation,
            RuntimeException exception) {

        log.error(
                "{} failed.",
                operation,
                exception
        );

        Notification.show(
                        "The operation could not be completed.",
                        5000,
                        Notification.Position.TOP_CENTER
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }

    private void showErrorNotification(
            String message) {

        Notification.show(
                        message,
                        4000,
                        Notification.Position.TOP_CENTER
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }
}