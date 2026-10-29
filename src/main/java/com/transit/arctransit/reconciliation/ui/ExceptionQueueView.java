package com.transit.arctransit.reconciliation.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
// common UI
import com.transit.arctransit.common.ui.FormInputs;

import com.transit.arctransit.common.ui.MainLayout;

// reconciliation
import com.transit.arctransit.reconciliation.ExceptionSummaryView;
import com.transit.arctransit.reconciliation.OperationalExceptionView;
import com.transit.arctransit.reconciliation.ReconciliationService;

// development scenario support
import com.transit.arctransit.reconciliation.application.ScenarioGeneratorService;

// vaadin
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import com.vaadin.flow.theme.lumo.LumoUtility;


// routing/security
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;

// spring
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.ObjectProvider;

// exception queue

@Route(
        value = "exceptions",
        layout = MainLayout.class
)
@PageTitle("Exception Queue")
@RolesAllowed("SYSTEM_ADMIN")
public class ExceptionQueueView
        extends VerticalLayout {

    // dependencies
    private final ReconciliationService reconciliationService;

    private final ObjectProvider<ScenarioGeneratorService>
            scenarioGeneratorProvider;

    // UI components
    private final Grid<OperationalExceptionView> grid = DataGrids.create();
    private final Span summarySpan = new Span();

    // constructor
    public ExceptionQueueView(
            ReconciliationService reconciliationService,
            ObjectProvider<ScenarioGeneratorService>
                    scenarioGeneratorProvider) {

        this.reconciliationService = reconciliationService;
        this.scenarioGeneratorProvider = scenarioGeneratorProvider;

        configureView();
        configureToolbar();
        configureGrid();
        configureScenarioToolbar();

        refreshGrid();
    }

    // view configuration
    private void configureView() {

        setSizeFull();
        setPadding(true);
        setSpacing(false);
    }

    // main toolbar
    private void configureToolbar() {

        H2 title =
                new H2(
                        "Operational Exception Queue"
                );

        summarySpan.addClassNames(
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextColor.ERROR
        );

        Button runButton =
                new Button(
                        "Run Reconciliation Engine",
                        event ->
                                runReconciliation()
                );

        runButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        HorizontalLayout toolbar =
                new HorizontalLayout(
                        title,
                        summarySpan,
                        new HorizontalLayout(runButton, new RefreshButton(this::refreshGrid)));

        toolbar.setWidthFull();

        toolbar.setJustifyContentMode(
                FlexComponent.JustifyContentMode.BETWEEN
        );

        toolbar.setDefaultVerticalComponentAlignment(
                FlexComponent.Alignment.CENTER
        );

        add(toolbar);
    }

    // development QA scenario toolbar
    private void configureScenarioToolbar() {

        ScenarioGeneratorService
                scenarioGenerator =
                scenarioGeneratorProvider
                        .getIfAvailable();

        // ScenarioGeneratorService
        if (scenarioGenerator == null) {
            return;
        }

        Span label =
                new Span("Development QA Scenarios:");

        Button contradictionButton =
                new Button(
                        "Simulate Contradiction",
                        event ->
                                generateContradiction(
                                        scenarioGenerator
                                )
                );

        contradictionButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY
        );

        Button incidentButton =
                new Button(
                        "Simulate Incident",
                        event ->
                                generateIncident(
                                        scenarioGenerator
                                )
                );

        incidentButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY
        );

        HorizontalLayout scenarioToolbar =
                new HorizontalLayout(
                        label,
                        contradictionButton,
                        incidentButton
                );

        scenarioToolbar.setPadding(false);
        scenarioToolbar.setSpacing(true);

        add(scenarioToolbar);
    }

    // grid configuration
    private void configureGrid() {

        grid.setSizeFull();
        grid.addItemDoubleClickListener(event -> openDetails(event.getItem()));

        grid.addColumn(
                OperationalExceptionView::id
        )
        .setHeader("ID")
        .setWidth("70px")
        .setFlexGrow(0);

        grid.addColumn(
                OperationalExceptionView::exceptionType
        )
        .setHeader("Type")
        .setWidth("150px")
        .setFlexGrow(0);

        grid.addColumn(OperationalExceptionView::ruleCode).setHeader("Rule");

        grid.addComponentColumn(
                this::createSeverityBadge
        )
        .setHeader("Severity")
        .setWidth("110px")
        .setFlexGrow(0);

        grid.addColumn(
                OperationalExceptionView::affectedEntityType
        )
        .setHeader("Entity Type")
        .setWidth("130px")
        .setFlexGrow(0);

        grid.addColumn(
                OperationalExceptionView::affectedEntityId
        )
        .setHeader("Entity ID")
        .setWidth("100px")
        .setFlexGrow(0);

        grid.addColumn(
                OperationalExceptionView::description
        )
        .setHeader("Description")
        .setFlexGrow(2);

        grid.addColumn(
                OperationalExceptionView::exceptionStatus
        )
        .setHeader("Status")
        .setWidth("140px")
        .setFlexGrow(0);

        grid.addColumn(
                exception ->
                        formatTimestamp(
                                exception.detectedAt()
                        )
        )
        .setHeader("Detected At")
        .setWidth("180px")
        .setFlexGrow(0);

        grid.addComponentColumn(
                this::createActionButtons
        )
        .setHeader("Actions")
        .setWidth("250px")
        .setFlexGrow(0);

        add(grid);

        expand(grid);
    }

    // severity badge
    private Span createSeverityBadge(
            OperationalExceptionView exception) {

        Span badge =
                new Span(
                        exception.severity()
                );

        switch (exception.severity()) {

            case "CRITICAL" -> {

                badge.addClassNames(
                        LumoUtility.Background.ERROR,
                        LumoUtility.TextColor.PRIMARY_CONTRAST,
                        LumoUtility.Padding.Horizontal.SMALL,
                        LumoUtility.BorderRadius.SMALL,
                        LumoUtility.FontWeight.BOLD
                );
            }

            case "HIGH" -> {

                badge.addClassNames(
                        LumoUtility.TextColor.ERROR,
                        LumoUtility.FontWeight.BOLD
                );
            }

            case "MEDIUM" -> {

                badge.addClassNames(
                        LumoUtility.TextColor.WARNING,
                        LumoUtility.FontWeight.BOLD
                );
            }

            default -> {

                badge.addClassNames(
                        LumoUtility.TextColor.SECONDARY
                );
            }
        }

        return badge;
    }

    // workflow action buttons
    private HorizontalLayout createActionButtons(
            OperationalExceptionView exception) {

        HorizontalLayout actions =
                new HorizontalLayout();

        actions.setSpacing(true);

        switch (exception.exceptionStatus()) {

            case "OPEN" -> {

                Button acknowledgeButton =
                        new Button(
                                "Acknowledge",
                                event ->
                                        acknowledgeException(
                                                exception.id()
                                        )
                        );

                acknowledgeButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL
                );

                actions.add(
                        acknowledgeButton
                );
            }

            case "ACKNOWLEDGED" -> {

                Button investigateButton =
                        new Button(
                                "Investigate",
                                event ->
                                        startInvestigation(
                                                exception.id()
                                        )
                        );

                investigateButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_CONTRAST
                );

                actions.add(
                        investigateButton
                );
            }

            case "INVESTIGATING" -> {

                Button resolveButton =
                        new Button(
                                "Resolve",
                                event ->
                                        openResolutionDialog(
                                                exception.id(),
                                                true
                                        )
                        );

                resolveButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_SUCCESS
                );

                Button dismissButton =
                        new Button(
                                "Dismiss",
                                event ->
                                        openResolutionDialog(
                                                exception.id(),
                                                false
                                        )
                        );

                dismissButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR
                );

                actions.add(
                        resolveButton,
                        dismissButton
                );
            }

            case "RESOLVED", "DISMISSED" -> {
                // terminal states have no remaining actions
            }

            default -> {
                // unknown status requires no UI action
            }
        }

        return actions;
    }

    // acknowledge exception
    private void acknowledgeException(
            Long exceptionId) {

        executeAction(
                () ->
                        reconciliationService
                                .acknowledgeException(
                                        exceptionId
                                ),
                "Exception acknowledged successfully."
        );
    }

    // start investigation

    private void startInvestigation(
            Long exceptionId) {

        executeAction(
                () ->
                        reconciliationService
                                .startInvestigation(
                                        exceptionId
                                ),
                "Investigation started successfully."
        );
    }

    // resolution dialog
    private void openResolutionDialog(
            Long exceptionId,
            boolean resolve) {

        Dialog dialog =
                Dialogs.create();

        dialog.setHeaderTitle(
                resolve
                        ? "Resolve Exception"
                        : "Dismiss Exception"
        );

        dialog.setWidth("400px");

        TextArea notesField =
                FormInputs.notes(new TextArea(
                        "Notes"
                ));

        notesField.setWidthFull();

        notesField.setRequiredIndicatorVisible(
                true
        );

        Button actionButton =
                new Button(
                        resolve
                                ? "Resolve"
                                : "Dismiss",
                        event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

                            String notes =
                                    notesField
                                            .getValue();

                            if (notes == null
                                    || notes.isBlank()) {

                                showError(
                                        "Notes are required."
                                );

                                return;
                            }

                            executeAction(
                                    () -> {

                                        if (resolve) {

                                            reconciliationService
                                                    .resolveException(
                                                            exceptionId,
                                                            notes.trim()
                                                    );

                                        } else {

                                            reconciliationService
                                                    .dismissException(
                                                            exceptionId,
                                                            notes.trim()
                                                    );
                                        }
                                    },
                                    resolve
                                            ? "Exception resolved successfully."
                                            : "Exception dismissed successfully."
                            );

                            dialog.close();
                        }
                );

        actionButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        if (resolve) {

            actionButton.addThemeVariants(
                    ButtonVariant.LUMO_SUCCESS
            );

        } else {

            actionButton.addThemeVariants(
                    ButtonVariant.LUMO_ERROR
            );
        }

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
                actionButton
        );

        dialog.open();
    }

    // run reconciliation
    private void runReconciliation() {

        executeAction(
                reconciliationService::runReconciliation,
                "Reconciliation run completed successfully."
        );
    }

    // generate contradiction scenario
    private void generateContradiction(
            ScenarioGeneratorService
                    scenarioGenerator) {

        executeAction(
                scenarioGenerator
                        ::generateContradictionScenario,
                "Contradiction scenario generated."
        );
    }

    // generate incident scenario
    private void generateIncident(
            ScenarioGeneratorService
                    scenarioGenerator) {

        executeAction(
                scenarioGenerator
                        ::generateIncidentScenario,
                "Incident scenario generated."
        );
    }

    // shared action executor
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

    // refresh exception queue
    private void refreshGrid() {

        grid.setItems(
                reconciliationService
                        .searchExceptions(
                                PageRequest.of(
                                        0,
                                        100
                                )
                        )
                        .getContent()
        );

        ExceptionSummaryView summary =
                reconciliationService
                        .getExceptionSummary();

        summarySpan.setText(
                summary.totalOpen()
                        + " Open Exceptions"
        );
    }

    // format timestamps
    private String formatTimestamp(
            java.time.Instant timestamp) {

        if (timestamp == null) {
            return "";
        }

        return timestamp
                .toString()
                .replace(
                        'T',
                        ' '
                )
                .substring(
                        0,
                        Math.min(
                                19,
                                timestamp
                                        .toString()
                                        .length()
                        )
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

        String safeMessage =
                message == null
                        || message.isBlank()
                        ? "The operation could not be completed."
                        : message;

        Notification.show(
                        safeMessage,
                        5000,
                        Notification.Position.TOP_END
                )
                .addThemeVariants(
                        NotificationVariant.LUMO_ERROR
                );
    }

    private void openDetails(OperationalExceptionView exception) {
        Dialog dialog = Dialogs.details("Exception Details");
        Dialogs.field(dialog, "Record ID", exception.id());
        Dialogs.field(dialog, "Type", exception.exceptionType());
        Dialogs.field(dialog, "Rule", exception.ruleCode());
        Dialogs.field(dialog, "Severity", exception.severity());
        Dialogs.field(dialog, "Affected record type", exception.affectedEntityType());
        Dialogs.field(dialog, "Affected record ID", exception.affectedEntityId());
        Dialogs.field(dialog, "Description", exception.description());
        Dialogs.field(dialog, "Status", exception.exceptionStatus());
        Dialogs.field(dialog, "Detected at", exception.detectedAt());
        Dialogs.field(dialog, "Acknowledged by", exception.acknowledgedBy());
        Dialogs.field(dialog, "Acknowledged at", exception.acknowledgedAt());
        Dialogs.field(dialog, "Resolved by", exception.resolvedBy());
        Dialogs.field(dialog, "Resolved at", exception.resolvedAt());
        Dialogs.field(dialog, "Notes", exception.resolutionNotes());
        dialog.open();
    }
}