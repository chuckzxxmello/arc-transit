package com.transit.arctransit.driver.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
// driver module

import com.transit.arctransit.common.ui.FormInputs;

import com.transit.arctransit.driver.CreateDriverCommand;
import com.transit.arctransit.driver.ChangeDriverAvailabilityCommand;
import com.transit.arctransit.driver.DriverAvailabilityHistoryView;
import com.vaadin.flow.component.textfield.TextArea;
import java.time.Instant;
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
import com.transit.arctransit.common.ui.ConfirmationDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
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
            DataGrids.create();


    private final Dialog detailsDialog = Dialogs.details("Driver Details");

    
    private String currentSearchFilter = "";

    // constructor

    public DriverManagementView(
            DriverManagementService driverService) {

        this.driverService = driverService;

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
                FormInputs.text(new TextField(), 200);

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
                        addButton,
                        new RefreshButton(this::refreshGrid)
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
        .setSortProperty("employeeNumber")
        .setSortable(true);

        grid.addColumn(
                DriverSummaryView::fullName
        )
        .setHeader("Name")
        .setSortProperty("firstName", "lastName")
        .setSortable(true);

        grid.addColumn(
                DriverSummaryView::licenseNumber
        )
        .setHeader("License #")
        .setSortProperty("licenseNumber")
        .setSortable(true);

        grid.addComponentColumn(
                this::createLicenseExpiryComponent
        )
        .setHeader("License Expiry")
        .setSortProperty("licenseExpiryDate")
        .setSortable(true);

        grid.addColumn(
                DriverSummaryView::employmentStatus
        )
        .setHeader("Employment Status")
        .setSortProperty("employmentStatus")
        .setSortable(true);

        grid.addColumn(DriverSummaryView::availability).setHeader("Reported Availability");
        grid.setItemsPageable(pageable -> driverService.searchDrivers(
                new DriverQuery(currentSearchFilter, null), pageable).getContent());

        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions").setWidth("410px").setFlexGrow(0);

        grid.addItemDoubleClickListener(event -> showDetails(event.getItem()));
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

        HorizontalLayout actions = new HorizontalLayout(
                editButton,
                statusButton,
                new Button("Availability", event -> openAvailabilityDialog(driver.id(), driver.availability())),
                archiveButton
        );
        actions.setSpacing(false);
        actions.getStyle().set("gap", "4px").set("flex-wrap", "wrap");
        actions.getChildren().filter(Button.class::isInstance).map(Button.class::cast)
                .forEach(button -> button.addThemeVariants(ButtonVariant.LUMO_SMALL));
        archiveButton.setTooltipText("Archive Driver");
        return actions;
    }

    // details

    private void showDetails(
            DriverSummaryView driver) {

        detailsDialog.removeAll();
        Dialogs.field(detailsDialog, "Record ID", driver.id());

        detailsDialog.add(
                new Paragraph(
                        "Employee No: "
                                + driver.employeeNumber()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Name: "
                                + driver.fullName()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "License: "
                                + driver.licenseNumber()
                )
        );

        detailsDialog.add(
                new Paragraph(
                        "Status: "
                                + driver.employmentStatus()
                )
        );

        Dialogs.field(detailsDialog, "Reported availability", driver.availability());
        detailsDialog.open();
    }

    // side panel



    private void closeDetails() {
        detailsDialog.close();
    }

    // create driver

    private void openCreateDialog() {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Register New Driver"
        );

        TextField employeeNumber =
                FormInputs.text(new TextField(
                        "Employee Number"
                ), 30);
        employeeNumber.setRequiredIndicatorVisible(true);

        TextField firstName =
                FormInputs.text(new TextField("First Name"), 50);
        firstName.setRequiredIndicatorVisible(true);

        TextField lastName =
                FormInputs.text(new TextField("Last Name"), 50);
        lastName.setRequiredIndicatorVisible(true);

        TextField licenseNumber =
                FormInputs.text(new TextField("License Number"), 50);
        licenseNumber.setRequiredIndicatorVisible(true);

        ComboBox<String> licenseType =
                FormInputs.choice(new ComboBox<>("License Type"));
        licenseType.setRequiredIndicatorVisible(true);

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
        expiryDate.setRequiredIndicatorVisible(true);

        TextField contactNumber =
                FormInputs.mobileNumber(new TextField(
                        "Contact Number"
                ));

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
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


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
                Dialogs.create();

        dialog.setHeaderTitle(
                "Edit Driver — "
                        + driver.firstName()
                        + " "
                        + driver.lastName()
        );

        TextField employeeNumber =
                FormInputs.text(new TextField(
                        "Employee Number"
                ), 30);
        employeeNumber.setRequiredIndicatorVisible(true);

        employeeNumber.setValue(
                driver.employeeNumber()
        );

        TextField firstName =
                FormInputs.text(new TextField("First Name"), 50);
        firstName.setRequiredIndicatorVisible(true);

        firstName.setValue(
                driver.firstName()
        );

        TextField lastName =
                FormInputs.text(new TextField("Last Name"), 50);
        lastName.setRequiredIndicatorVisible(true);

        lastName.setValue(
                driver.lastName()
        );

        TextField licenseNumber =
                FormInputs.text(new TextField(
                        "License Number"
                ), 50);
        licenseNumber.setRequiredIndicatorVisible(true);

        licenseNumber.setValue(
                driver.licenseNumber()
        );

        ComboBox<String> licenseType =
                FormInputs.choice(new ComboBox<>("License Type"));
        licenseType.setRequiredIndicatorVisible(true);

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
        expiryDate.setRequiredIndicatorVisible(true);

        expiryDate.setValue(
                LocalDate.parse(
                        driver.licenseExpiryDate()
                )
        );

        TextField contactNumber =
                FormInputs.mobileNumber(new TextField(
                        "Contact Number"
                ));

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
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


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

    private void openAvailabilityDialog(Long driverId, String currentAvailability) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Driver Availability");
        ComboBox<String> availability = FormInputs.choice(new ComboBox<>("Reported Availability"));
        availability.setRequiredIndicatorVisible(true);
        availability.setItems("AVAILABLE", "UNAVAILABLE", "UNKNOWN");
        availability.setValue(currentAvailability);
        TextArea reason = FormInputs.notes(new TextArea("Reason"));
        reason.setRequiredIndicatorVisible(true);
        Button record = new Button("Record Availability", event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

            try {
                driverService.changeAvailability(new ChangeDriverAvailabilityCommand(
                        driverId, availability.getValue(), Instant.now(), reason.getValue()));
                dialog.close();
                refreshGrid();
                showSuccess("Availability report recorded as unverified.");
            } catch (RuntimeException exception) {
                showError("Record availability", exception);
            }
        });
        record.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(new VerticalLayout(availability, reason));
        dialog.getFooter().add(new Button("History", event -> openAvailabilityHistory(driverId)),
                new Button("Close", event -> dialog.close()), record);
        dialog.open();
    }

    private void openAvailabilityHistory(Long driverId) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Driver Availability History");
        dialog.setWidth("1000px");
        Grid<DriverAvailabilityHistoryView> history = DataGrids.create();
        history.addColumn(DriverAvailabilityHistoryView::previousAvailability).setHeader("Previous");
        history.addColumn(DriverAvailabilityHistoryView::availability).setHeader("Reported Availability");
        history.addColumn(DriverAvailabilityHistoryView::effectiveAt).setHeader("Effective Time");
        history.addColumn(DriverAvailabilityHistoryView::recordedAt).setHeader("Recorded Time");
        history.addColumn(DriverAvailabilityHistoryView::recordedBy).setHeader("Recorded By");
        history.addColumn(DriverAvailabilityHistoryView::sourceType).setHeader("Source");
        history.addColumn(DriverAvailabilityHistoryView::verificationStatus).setHeader("Verification");
        history.addColumn(DriverAvailabilityHistoryView::reason).setHeader("Reason");
        history.setItemsPageable(pageable -> driverService.availabilityHistory(driverId, pageable).getContent());
        dialog.add(history);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    // employment status

    private void openStatusDialog(
            Long driverId,
            String currentStatus) {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Change Employment Status"
        );

        ComboBox<String> statusCombo =
                FormInputs.choice(new ComboBox<>("New Status"));
        statusCombo.setRequiredIndicatorVisible(true);

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
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


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

        ConfirmationDialog dialog =
                new ConfirmationDialog();

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

                        closeDetails();

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
        grid.getDataProvider().refreshAll();
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
        com.transit.arctransit.common.ui.ApplicationErrors.show(exception);
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
