package com.transit.arctransit.maintenance.ui;

// maintenance module
import com.transit.arctransit.maintenance.CreateMaintenanceCommand;
import com.transit.arctransit.maintenance.MaintenanceRecordView;
import com.transit.arctransit.maintenance.MaintenanceService;

// common UI
import com.transit.arctransit.common.ui.MainLayout;

// vaadin
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

// security
import jakarta.annotation.security.RolesAllowed;

// spring
import org.springframework.data.domain.PageRequest;

// maintenance management view

@Route(
        value = "maintenance",
        layout = MainLayout.class
)
@PageTitle("Maintenance Management")
@RolesAllowed("SYSTEM_ADMIN")
public class MaintenanceManagementView
        extends VerticalLayout {

    // dependencies
    private final MaintenanceService maintenanceService;

    // grid
    private final Grid<MaintenanceRecordView> grid =
            new Grid<>();

    // constructor
    public MaintenanceManagementView(
            MaintenanceService maintenanceService) {

        this.maintenanceService =
                maintenanceService;

        configureView();

        HorizontalLayout toolbar =
                createToolbar();

        configureGrid();

        add(
                toolbar,
                grid
        );

        expand(grid);

        refreshGrid();
    }

    // view configuration
    private void configureView() {

        setSizeFull();
        setPadding(true);
    }

    // toolbar
    private HorizontalLayout createToolbar() {

        H2 title =
                new H2(
                        "Maintenance Management"
                );

        Button requestButton =
                new Button(
                        "Request Maintenance",
                        event ->
                                openRequestDialog()
                );

        requestButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        HorizontalLayout toolbar =
                new HorizontalLayout(
                        title,
                        requestButton
                );

        toolbar.setWidthFull();

        toolbar.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        toolbar.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        return toolbar;
    }

    // grid
    private void configureGrid() {

        grid.setSizeFull();

        grid.addColumn(
                MaintenanceRecordView::id
        )
        .setHeader("ID")
        .setWidth("70px")
        .setFlexGrow(0);

        grid.addColumn(
                MaintenanceRecordView::fleetUnitId
        )
        .setHeader("Fleet Unit")
        .setWidth("110px");

        grid.addColumn(
                MaintenanceRecordView::maintenanceType
        )
        .setHeader("Type")
        .setWidth("130px");

        grid.addColumn(
                MaintenanceRecordView::maintenanceStatus
        )
        .setHeader("Status")
        .setWidth("130px");

        grid.addColumn(
                MaintenanceRecordView::description
        )
        .setHeader("Description")
        .setFlexGrow(2);

        grid.addColumn(
                record ->
                        record.scheduledDate() == null
                                ? ""
                                : record
                                        .scheduledDate()
                                        .toString()
        )
        .setHeader("Scheduled Date")
        .setWidth("140px");

        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions")
        .setWidth("260px")
        .setFlexGrow(0);
    }

    // workflow actions
    private HorizontalLayout createActionButtons(
            MaintenanceRecordView record) {

        HorizontalLayout actions =
                new HorizontalLayout();

        switch (record.maintenanceStatus()) {

            case "REQUESTED" -> {

                Button approveButton =
                        new Button(
                                "Approve",
                                event ->
                                        approve(
                                                record.id()
                                        )
                        );

                approveButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL
                );

                Button cancelButton =
                        new Button(
                                "Cancel",
                                event ->
                                        cancel(
                                                record.id()
                                        )
                        );

                cancelButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR
                );

                actions.add(
                        approveButton,
                        cancelButton
                );
            }

            case "APPROVED" -> {

                Button startButton =
                        new Button(
                                "Start Work",
                                event ->
                                        startMaintenance(
                                                record.id()
                                        )
                        );

                startButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_PRIMARY
                );

                actions.add(startButton);
            }

            case "IN_PROGRESS" -> {

                Button completeButton =
                        new Button(
                                "Complete",
                                event ->
                                        openCompleteDialog(
                                                record.id()
                                        )
                        );

                completeButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_SUCCESS
                );

                actions.add(
                        completeButton
                );
            }

            default -> {
                // COMPLETED and CANCELLED have no active actions
            }
        }

        return actions;
    }

    // request maintenance
    private void openRequestDialog() {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Request Maintenance"
        );

        dialog.setWidth("500px");

        IntegerField fleetUnitId =
                new IntegerField(
                        "Fleet Unit ID"
                );

        fleetUnitId.setWidthFull();
        fleetUnitId.setMin(1);

        ComboBox<String> maintenanceType =
                new ComboBox<>(
                        "Maintenance Type"
                );

        maintenanceType.setItems(
                "SCHEDULED",
                "CORRECTIVE",
                "EMERGENCY"
        );

        maintenanceType.setValue(
                "SCHEDULED"
        );

        maintenanceType.setWidthFull();

        TextArea description =
                new TextArea(
                        "Description"
                );

        description.setWidthFull();

        DatePicker scheduledDate =
                new DatePicker(
                        "Scheduled Date"
                );

        scheduledDate.setWidthFull();

        VerticalLayout form =
                new VerticalLayout(
                        fleetUnitId,
                        maintenanceType,
                        description,
                        scheduledDate
                );

        form.setPadding(false);

        Button requestButton =
                new Button(
                        "Request",
                        event -> {

                            if (fleetUnitId.getValue() == null) {

                                showError(
                                        "Fleet unit ID is required."
                                );

                                return;
                            }

                            if (description
                                    .getValue()
                                    .isBlank()) {

                                showError(
                                        "Description is required."
                                );

                                return;
                            }

                            try {

                                maintenanceService
                                        .requestMaintenance(
                                                new CreateMaintenanceCommand(
                                                        fleetUnitId
                                                                .getValue()
                                                                .longValue(),
                                                        maintenanceType.getValue(),
                                                        description.getValue(),
                                                        scheduledDate.getValue()
                                                )
                                        );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Maintenance requested successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        exception.getMessage()
                                );
                            }
                        }
                );

        requestButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        Button cancelButton =
                new Button(
                        "Cancel",
                        event ->
                                dialog.close()
                );

        dialog.add(form);

        dialog.getFooter().add(
                cancelButton,
                requestButton
        );

        dialog.open();
    }

    // approve
    private void approve(
            Long recordId) {

        executeAction(
                () ->
                        maintenanceService
                                .approveMaintenance(
                                        recordId
                                ),
                "Maintenance approved successfully."
        );
    }

    // start
    private void startMaintenance(
            Long recordId) {

        executeAction(
                () ->
                        maintenanceService
                                .startMaintenance(
                                        recordId
                                ),
                "Maintenance started successfully."
        );
    }

    // cancel
    private void cancel(
            Long recordId) {

        executeAction(
                () ->
                        maintenanceService
                                .cancelMaintenance(
                                        recordId
                                ),
                "Maintenance cancelled successfully."
        );
    }

    // complete
    private void openCompleteDialog(
            Long recordId) {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Complete Maintenance"
        );

        dialog.setWidth("400px");

        TextArea technicianNotes =
                new TextArea(
                        "Technician Notes"
                );

        technicianNotes.setWidthFull();

        technicianNotes.setRequiredIndicatorVisible(
                true
        );

        Button completeButton =
                new Button(
                        "Complete",
                        event -> {

                            if (technicianNotes
                                    .getValue()
                                    .isBlank()) {

                                showError(
                                        "Technician notes are required."
                                );

                                return;
                            }

                            try {

                                maintenanceService
                                        .completeMaintenance(
                                                recordId,
                                                technicianNotes.getValue()
                                        );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Maintenance completed successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        exception.getMessage()
                                );
                            }
                        }
                );

        completeButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SUCCESS
        );

        Button cancelButton =
                new Button(
                        "Cancel",
                        event ->
                                dialog.close()
                );

        dialog.add(
                technicianNotes
        );

        dialog.getFooter().add(
                cancelButton,
                completeButton
        );

        dialog.open();
    }

    // shared lifecycle action
    private void executeAction(
            Runnable action,
            String successMessage) {

        try {

            action.run();

            refreshGrid();

            showSuccess(
                    successMessage
            );

        } catch (RuntimeException exception) {

            showError(
                    exception.getMessage()
            );
        }
    }

    // refresh
    private void refreshGrid() {

        grid.setItems(
                maintenanceService
                        .searchRecords(
                                PageRequest.of(
                                        0,
                                        100
                                )
                        )
                        .getContent()
        );
    }

    // success notification
    private void showSuccess(
            String message) {

        Notification.show(
                        message,
                        3000,
                        Notification.Position.TOP_END
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_SUCCESS
                );
    }

    // error notification
    private void showError(
            String message) {

        Notification.show(
                        message == null
                                || message.isBlank()
                                ? "The operation could not be completed."
                                : message,
                        5000,
                        Notification.Position.TOP_END
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }
}