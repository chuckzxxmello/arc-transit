package com.transit.arctransit.incident.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
// incident module
import com.transit.arctransit.common.ui.FormInputs;
import com.transit.arctransit.common.ui.RecordPicker;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;

import com.transit.arctransit.incident.CreateIncidentCommand;
import com.transit.arctransit.incident.IncidentManagementService;
import com.transit.arctransit.incident.IncidentView;

// common UI
import com.transit.arctransit.common.ui.MainLayout;

// vaadin
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Paragraph;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

// security
import jakarta.annotation.security.RolesAllowed;

// spring
import org.springframework.data.domain.PageRequest;

// incident management view
@Route(
        value = "incidents",
        layout = MainLayout.class
)
@PageTitle("Incident Management")
@RolesAllowed("SYSTEM_ADMIN")
public class IncidentManagementView
        extends VerticalLayout {

    // dependencies
    private final IncidentManagementService
            incidentService;
    private final FleetManagementService fleetService;

    // grid
    private final Grid<IncidentView> grid =
            DataGrids.create();

    // constructor
    public IncidentManagementView(
            IncidentManagementService incidentService,
            FleetManagementService fleetService) {

        this.incidentService =
                incidentService;
        this.fleetService = fleetService;

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
                        "Incident Management"
                );

        Button reportButton =
                new Button(
                        "Report Incident",
                        event ->
                                openReportDialog()
                );

        reportButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        HorizontalLayout toolbar =
                new HorizontalLayout(
                        title,
                        new HorizontalLayout(reportButton, new RefreshButton(this::refreshGrid)));

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
        grid.addItemDoubleClickListener(event -> openDetails(event.getItem()));

        grid.addColumn(
                IncidentView::id
        )
        .setHeader("ID")
        .setWidth("70px")
        .setFlexGrow(0);

        grid.addColumn(
                IncidentView::title
        )
        .setHeader("Title")
        .setFlexGrow(2);

        grid.addColumn(
                IncidentView::severity
        )
        .setHeader("Severity")
        .setWidth("110px");

        grid.addColumn(
                IncidentView::status
        )
        .setHeader("Status")
        .setWidth("140px");

        grid.addColumn(
                IncidentView::affectedServiceType
        )
        .setHeader("Affected Service")
        .setWidth("140px");

        grid.addColumn(
                IncidentView::reportedBy
        )
        .setHeader("Reported By")
        .setWidth("140px");

        grid.addColumn(
                incident ->
                        incident.reportedAt() == null
                                ? ""
                                : incident
                                        .reportedAt()
                                        .toString()
                                        .substring(
                                                0,
                                                19
                                        )
        )
        .setHeader("Reported At")
        .setWidth("180px");

        grid.addColumn(IncidentView::effectiveAt).setHeader("Effective At (UTC)");
        grid.addColumn(IncidentView::operationalReportId).setHeader("Report ID");
        grid.addColumn(IncidentView::maintenanceRecordId).setHeader("Maintenance ID");
        grid.addComponentColumn(incident -> new Button("History", event -> openHistory(incident.id())));
        grid.addComponentColumn(incident -> new Button(
                incident.maintenanceRecordId() == null ? "Link Maintenance" : "Unlink Maintenance",
                event -> {
                    if (incident.maintenanceRecordId() == null) openMaintenanceDialog(incident.id());
                    else openUnlinkMaintenanceDialog(incident);
                })).setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions")
        .setWidth("300px")
        .setFlexGrow(0);
    }

    // workflow action buttons
    private HorizontalLayout createActionButtons(
            IncidentView incident) {

        HorizontalLayout actions =
                new HorizontalLayout();

        switch (incident.status()) {

            case "REPORTED" -> {

                Button button =
                        new Button(
                                "Acknowledge",
                                event ->
                                        acknowledge(
                                                incident.id()
                                        )
                        );

                button.addThemeVariants(
                        ButtonVariant.LUMO_SMALL
                );

                actions.add(button);
            }

            case "ACKNOWLEDGED" -> {

                Button button =
                        new Button(
                                "Investigate",
                                event ->
                                        startInvestigation(
                                                incident.id()
                                        )
                        );

                button.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_CONTRAST
                );

                actions.add(button);
            }

            case "INVESTIGATING" -> {

                Button button =
                        new Button(
                                "Resolve",
                                event ->
                                        openResolveDialog(
                                                incident.id()
                                        )
                        );

                button.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_SUCCESS
                );

                actions.add(button);
            }

            case "RESOLVED" -> {

                Button button =
                        new Button(
                                "Close",
                                event ->
                                        close(
                                                incident.id()
                                        )
                        );

                button.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_PRIMARY
                );

                actions.add(button);
            }

            default -> {
                // CLOSED has no remaining workflow action
            }
        }

        return actions;
    }

    // report incident
    private void openReportDialog() {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Report New Incident"
        );

        dialog.setWidth("500px");

        TextField titleField =
                FormInputs.text(new TextField("Title"), 200);

        titleField.setWidthFull();
        titleField.setRequiredIndicatorVisible(true);

        TextArea descriptionField =
                FormInputs.notes(new TextArea("Description"));

        descriptionField.setWidthFull();

        ComboBox<String> severityField =
                FormInputs.choice(new ComboBox<>("Severity"));

        severityField.setItems(
                "LOW",
                "MEDIUM",
                "HIGH",
                "CRITICAL"
        );

        severityField.setValue(
                "MEDIUM"
        );

        severityField.setWidthFull();
        severityField.setRequiredIndicatorVisible(true);

        ComboBox<String> serviceField =
                FormInputs.choice(new ComboBox<>(
                        "Affected Service"
                ));

        serviceField.setItems(
                "FLEET",
                "DRIVER",
                "ROUTE",
                "DISPATCH"
        );

        serviceField.setWidthFull();

        serviceField.setClearButtonVisible(
                true
        );

        TextField affectedIdField = FormInputs.recordId(new TextField("Affected Record ID (optional)"));
        affectedIdField.setEnabled(false);
        ComboBox<RecordPicker.Option> affectedBus = RecordPicker.create("Affected Bus",
                (filter, pageable) -> fleetService.searchUnits(new FleetUnitQuery(filter, null), pageable)
                        .map(bus -> new RecordPicker.Option(bus.id(), bus.unitNumber() + " (" + bus.plateNumber() + ")"))
                        .getContent());
        affectedBus.setPlaceholder("Type ARC-901 or a plate and select the bus");
        affectedBus.setVisible(false);
        serviceField.addValueChangeListener(event -> {
            affectedBus.clear();
            affectedIdField.clear();
            boolean fleet = "FLEET".equals(event.getValue());
            affectedBus.setVisible(fleet);
            affectedBus.setRequiredIndicatorVisible(fleet);
            affectedIdField.setVisible(!fleet);
            affectedIdField.setEnabled(event.getValue() != null && !fleet);
        });
        TextField reportIdField = FormInputs.recordId(new TextField("Operational Report ID (optional)"));
        DateTimePicker effectiveAtField = new DateTimePicker("Effective At (Asia/Manila)");
        effectiveAtField.setRequiredIndicatorVisible(true);
        effectiveAtField.setValue(LocalDateTime.now(ZoneId.of("Asia/Manila")));

        VerticalLayout form =
                new VerticalLayout(
                        titleField,
                        descriptionField,
                        severityField,
                        serviceField,
                        affectedIdField,
                        affectedBus,
                        reportIdField,
                        effectiveAtField
                );

        form.setPadding(false);

        Button saveButton =
                new Button(
                        "Report",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


                            if (titleField
                                    .getValue()
                                    .isBlank()) {

                                showError(
                                        "Title is required."
                                );

                                return;
                            }

                            try {

                                incidentService.reportIncident(
                                        new CreateIncidentCommand(
                                                titleField.getValue(),
                                                descriptionField.getValue(),
                                                severityField.getValue(),
                                                serviceField.getValue(),
                                                "FLEET".equals(serviceField.getValue())
                                                        ? affectedBus.getValue() == null ? null : affectedBus.getValue().id()
                                                        : optionalId(affectedIdField.getValue()),
                                                effectiveAtField.getValue() == null ? null
                                                        : effectiveAtField.getValue().atZone(ZoneId.of("Asia/Manila")).toInstant(),
                                                optionalId(reportIdField.getValue())
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Incident reported successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        com.transit.arctransit.common.ui.ApplicationErrors.message(exception)
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

        dialog.add(form);

        dialog.getFooter().add(
                cancelButton,
                saveButton
        );

        dialog.open();
    }

    private static Long optionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(value.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new com.transit.arctransit.common.exception.CommandValidationException(
                    "Record IDs must be positive integers.");
        }
    }

    private void openHistory(Long id) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Incident History");
        VerticalLayout entries = new VerticalLayout();
        for (var event : incidentService.getHistory(id)) {
            entries.add(new Paragraph(event.recordedAt() + " | " + event.recordedBy() + " | "
                    + event.newStatus() + " | " + event.reason()));
        }
        dialog.add(entries);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private void openMaintenanceDialog(Long id) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Link Maintenance Record");
        TextField maintenanceId = FormInputs.recordId(new TextField("Maintenance Record ID"));
        maintenanceId.setRequiredIndicatorVisible(true);
        dialog.add(maintenanceId);
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), new Button("Link", event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

            try {
                Long recordId = optionalId(maintenanceId.getValue());
                if (recordId == null) {
                    throw new com.transit.arctransit.common.exception.CommandValidationException(
                            "Maintenance record ID is required.");
                }
                incidentService.linkMaintenanceRecord(id, recordId);
                dialog.close();
                refreshGrid();
            } catch (com.transit.arctransit.common.exception.CommandValidationException
                    | com.transit.arctransit.common.exception.ResourceNotFoundException exception) {
                showError(com.transit.arctransit.common.ui.ApplicationErrors.message(exception));
            }
        }));
        dialog.open();
    }

    // resolve incident
    private void openUnlinkMaintenanceDialog(IncidentView incident) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Unlink Maintenance Record");
        dialog.setWidth("560px");
        TextArea reason = FormInputs.notes(new TextArea("Reason for Unlinking"));
        reason.setRequiredIndicatorVisible(true);
        reason.setWidthFull();
        dialog.add(new com.vaadin.flow.component.html.Paragraph("Remove the link to maintenance record "
                + incident.maintenanceRecordId() + "? The maintenance record and both histories remain."), reason);
        Button unlink = new Button("Unlink", event -> {
            if (!FormInputs.validate(dialog)) return;
            try {
                incidentService.unlinkMaintenanceRecord(incident.id(), incident.maintenanceRecordId(), reason.getValue());
                dialog.close();
                refreshGrid();
            } catch (RuntimeException exception) {
                com.transit.arctransit.common.ui.ApplicationErrors.show(exception);
            }
        });
        unlink.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), unlink);
        dialog.open();
    }

    private void openResolveDialog(
            Long incidentId) {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                "Resolve Incident"
        );

        dialog.setWidth("400px");

        TextArea notesField =
                FormInputs.notes(new TextArea(
                        "Resolution Notes"
                ));

        notesField.setWidthFull();
        notesField.setRequiredIndicatorVisible(true);

        Button resolveButton =
                new Button(
                        "Resolve",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }


                            if (notesField
                                    .getValue()
                                    .isBlank()) {

                                showError(
                                        "Resolution notes are required."
                                );

                                return;
                            }

                            try {

                                incidentService.resolveIncident(
                                        incidentId,
                                        notesField.getValue()
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Incident resolved successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        com.transit.arctransit.common.ui.ApplicationErrors.message(exception)
                                );
                            }
                        }
                );

        resolveButton.addThemeVariants(
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
                notesField
        );

        dialog.getFooter().add(
                cancelButton,
                resolveButton
        );

        dialog.open();
    }

    // acknowledge
    private void acknowledge(
            Long incidentId) {

        executeAction(
                () ->
                        incidentService
                                .acknowledgeIncident(
                                        incidentId
                                ),
                "Incident acknowledged successfully."
        );
    }

    // start investigation
    private void startInvestigation(
            Long incidentId) {

        executeAction(
                () ->
                        incidentService
                                .startInvestigation(
                                        incidentId
                                ),
                "Investigation started successfully."
        );
    }

    // close incident
    private void close(
            Long incidentId) {

        executeAction(
                () ->
                        incidentService
                                .closeIncident(
                                        incidentId
                                ),
                "Incident closed successfully."
        );
    }

    // shared action execution
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
                    com.transit.arctransit.common.ui.ApplicationErrors.message(exception)
            );
        }
    }

    // refresh
    private void refreshGrid() {

        grid.setItems(
                incidentService
                        .searchIncidents(
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

    private void openDetails(IncidentView incident) {
        Dialog dialog = Dialogs.details("Incident Details");
        Dialogs.field(dialog, "Record ID", incident.id());
        Dialogs.field(dialog, "Title", incident.title());
        Dialogs.field(dialog, "Description", incident.description());
        Dialogs.field(dialog, "Severity", incident.severity());
        Dialogs.field(dialog, "Status", incident.status());
        Dialogs.field(dialog, "Affected service", incident.affectedServiceType());
        Dialogs.field(dialog, "Affected record ID", incident.affectedEntityId());
        Dialogs.field(dialog, "Reported by", incident.reportedBy());
        Dialogs.field(dialog, "Reported at", incident.reportedAt());
        Dialogs.field(dialog, "Effective at", incident.effectiveAt());
        Dialogs.field(dialog, "Source", incident.sourceType());
        Dialogs.field(dialog, "Acknowledged at", incident.acknowledgedAt());
        Dialogs.field(dialog, "Resolved at", incident.resolvedAt());
        Dialogs.field(dialog, "Resolution notes", incident.resolutionNotes());
        Dialogs.field(dialog, "Operational report ID", incident.operationalReportId());
        Dialogs.field(dialog, "Maintenance record ID", incident.maintenanceRecordId());
        dialog.open();
    }
}
