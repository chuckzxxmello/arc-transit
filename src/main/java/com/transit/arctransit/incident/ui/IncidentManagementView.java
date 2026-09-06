package com.transit.arctransit.incident.ui;

// incident module
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
import com.vaadin.flow.component.grid.Grid;
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

    // grid
    private final Grid<IncidentView> grid =
            new Grid<>();

    // constructor
    public IncidentManagementView(
            IncidentManagementService incidentService) {

        this.incidentService =
                incidentService;

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
                        reportButton
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
                new Dialog();

        dialog.setHeaderTitle(
                "Report New Incident"
        );

        dialog.setWidth("500px");

        TextField titleField =
                new TextField("Title");

        titleField.setWidthFull();
        titleField.setRequiredIndicatorVisible(true);

        TextArea descriptionField =
                new TextArea("Description");

        descriptionField.setWidthFull();

        ComboBox<String> severityField =
                new ComboBox<>("Severity");

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
                new ComboBox<>(
                        "Affected Service"
                );

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

        VerticalLayout form =
                new VerticalLayout(
                        titleField,
                        descriptionField,
                        severityField,
                        serviceField
                );

        form.setPadding(false);

        Button saveButton =
                new Button(
                        "Report",
                        event -> {

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
                                                null
                                        )
                                );

                                dialog.close();

                                refreshGrid();

                                showSuccess(
                                        "Incident reported successfully."
                                );

                            } catch (RuntimeException exception) {

                                showError(
                                        exception.getMessage()
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

    // resolve incident
    private void openResolveDialog(
            Long incidentId) {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Resolve Incident"
        );

        dialog.setWidth("400px");

        TextArea notesField =
                new TextArea(
                        "Resolution Notes"
                );

        notesField.setWidthFull();
        notesField.setRequiredIndicatorVisible(true);

        Button resolveButton =
                new Button(
                        "Resolve",
                        event -> {

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
                                        exception.getMessage()
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
                    exception.getMessage()
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
}