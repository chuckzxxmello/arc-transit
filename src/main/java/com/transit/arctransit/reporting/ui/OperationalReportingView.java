package com.transit.arctransit.reporting.ui;

import com.transit.arctransit.common.ui.RefreshButton;
import com.transit.arctransit.common.ui.Dialogs;
import com.transit.arctransit.common.ui.ConfirmationDialog;
import com.transit.arctransit.common.ui.ApplicationErrors;
import com.transit.arctransit.common.ui.FormInputs;
import com.transit.arctransit.common.ui.RecordPicker;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitQuery;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverQuery;
import com.transit.arctransit.route.RouteManagementService;
import com.transit.arctransit.route.RouteQuery;
import com.transit.arctransit.dispatch.DispatchService;

import com.transit.arctransit.common.ui.MainLayout;
import com.transit.arctransit.reporting.CreateOperationalReportCommand;
import com.transit.arctransit.reporting.OperationalReportService;
import com.transit.arctransit.reporting.OperationalReportView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.transit.arctransit.common.ui.DataGrids;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Operational Reports")
@RolesAllowed({"SYSTEM_ADMIN", "OPERATIONS_STAFF"})
public class OperationalReportingView extends VerticalLayout {

    private final OperationalReportService reportService;
    private final FleetManagementService fleetService;
    private final DriverManagementService driverService;
    private final RouteManagementService routeService;
    private final DispatchService dispatchService;
    private final java.util.Map<String, String> recordLabels = new java.util.HashMap<>();
    private final Grid<OperationalReportView> grid = DataGrids.create();

    public OperationalReportingView(OperationalReportService reportService, FleetManagementService fleetService,
            DriverManagementService driverService, RouteManagementService routeService, DispatchService dispatchService) {
        this.reportService = reportService;
        this.fleetService = fleetService;
        this.driverService = driverService;
        this.routeService = routeService;
        this.dispatchService = dispatchService;
        setSizeFull();
        Button submit = new Button("Record Report", event -> openReportDialog());
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        H2 title = new H2("Operational Reports");
        title.getStyle().set("margin", "0");
        HorizontalLayout header = new HorizontalLayout(title,
                new HorizontalLayout(submit, new RefreshButton(this::refreshGrid)));
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        add(header, new Paragraph("Log information received by staff, phone or radio about a bus, driver, route or trip. "
                + "Keep its source and event time for administrator review. A report does not automatically change "
                + "availability or establish what physically happened."));
        grid.setSizeFull();
        grid.addItemDoubleClickListener(event -> openDetails(event.getItem()));
        grid.addColumn(OperationalReportView::id).setHeader("ID");
        grid.addColumn(this::recordLabel).setHeader("Affected Record").setWidth("240px");
        grid.addColumn(OperationalReportView::description).setHeader("Report").setWidth("320px");
        grid.addColumn(OperationalReportView::sourceType).setHeader("Source");
        grid.addColumn(OperationalReportView::reportedBy).setHeader("Recorded By");
        grid.addColumn(OperationalReportView::effectiveAt).setHeader("Effective At (UTC)");
        grid.addColumn(OperationalReportView::reportedAt).setHeader("Reported At (UTC)");
        grid.addColumn(OperationalReportView::freshness).setHeader("Information Age");
        grid.addColumn(OperationalReportView::verificationStatus).setHeader("Verification");
        grid.addComponentColumn(report -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.add(new Button("History", event -> openHistory(report.id())));
            if (isAdmin() && ("UNVERIFIED".equals(report.verificationStatus())
                    || "DISPUTED".equals(report.verificationStatus()))) {
                actions.add(new Button("Review", event -> openVerificationDialog(report.id())));
            }
            if (isAdmin()) {
                actions.add(new Button("Archive", event -> openArchiveDialog(report.id())));
            }
            return actions;
        }).setHeader("Actions");
        add(grid);
        expand(grid);
        refreshGrid();
    }

    private void openArchiveDialog(Long id) {
        ConfirmationDialog dialog = new ConfirmationDialog();
        dialog.setHeader("Archive Operational Report " + id + "?");
        dialog.setText("Move this report to Archived Data > Archived Operational Reports. "
                + "Its verification status, notes and history remain. You can restore it from the archive.");
        dialog.setConfirmText("Archive Report");
        dialog.addConfirmListener(event -> {
            try {
                reportService.archiveReport(id);
                refreshGrid();
                Notification.show("Operational report archived.");
            } catch (RuntimeException exception) {
                ApplicationErrors.show(exception);
            }
        });
        dialog.open();
    }

    private void refreshGrid() {
        recordLabels.clear();
        grid.setItems(query -> reportService.searchReports(PageRequest.of(query.getPage(), query.getPageSize(),
                Sort.by(Sort.Direction.DESC, "reportedAt").and(Sort.by(Sort.Direction.DESC, "id")))).stream());
    }

    private void openReportDialog() {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Record Operational Report");
        dialog.setWidth("500px");
        ComboBox<String> source = FormInputs.choice(new ComboBox<>("Source"));
        source.setRequiredIndicatorVisible(true);
        source.setItems("STAFF_ENTRY", "PHONE", "RADIO");
        source.setValue("STAFF_ENTRY");
        ComboBox<String> entityType = FormInputs.choice(new ComboBox<>("Affected Record Type"));
        entityType.setRequiredIndicatorVisible(true);
        entityType.setItems("FLEET", "DRIVER", "ROUTE", "DISPATCH");
        entityType.setItemLabelGenerator(type -> "FLEET".equals(type) ? "Bus" : switch (type) {
            case "DRIVER" -> "Driver";
            case "ROUTE" -> "Route";
            default -> "Dispatch / trip";
        });
        ComboBox<RecordPicker.Option> affectedRecord = RecordPicker.create("Affected Record", (filter, pageable) -> {
            if (entityType.getValue() == null) return java.util.List.of();
            return switch (entityType.getValue()) {
                case "FLEET" -> fleetService.searchUnits(new FleetUnitQuery(filter, null), pageable)
                        .map(bus -> new RecordPicker.Option(bus.id(), bus.unitNumber() + " (" + bus.plateNumber() + ")")).getContent();
                case "DRIVER" -> driverService.searchDrivers(new DriverQuery(filter, null), pageable)
                        .map(driver -> new RecordPicker.Option(driver.id(), driver.employeeNumber() + " - " + driver.fullName())).getContent();
                case "ROUTE" -> routeService.searchRoutes(new RouteQuery(filter, null), pageable)
                        .map(route -> new RecordPicker.Option(route.id(), route.routeCode() + " - " + route.routeName())).getContent();
                default -> dispatchService.searchReportCandidates(filter, pageable)
                        .map(trip -> new RecordPicker.Option(trip.id(), "Dispatch #" + trip.id() + " - "
                                + trip.dispatchDate() + " - " + trip.fleetUnitNumber() + " - " + trip.routeCode())).getContent();
            };
        });
        affectedRecord.setRequiredIndicatorVisible(true);
        affectedRecord.setEnabled(false);
        affectedRecord.setHelperText("Choose a record type, then search and select the actual record. No database ID is needed.");
        entityType.addValueChangeListener(event -> {
            affectedRecord.clear();
            affectedRecord.setEnabled(event.getValue() != null);
            affectedRecord.getDataProvider().refreshAll();
            affectedRecord.setPlaceholder(switch (event.getValue() == null ? "" : event.getValue()) {
                case "FLEET" -> "Search ARC-001 or a plate number";
                case "DRIVER" -> "Search employee number, name or license";
                case "ROUTE" -> "Search route code or name";
                case "DISPATCH" -> "Search bus number, route, driver or date";
                default -> "Choose the affected record type first";
            });
        });
        DateTimePicker effectiveAt = new DateTimePicker("Effective At (Asia/Manila)");
        effectiveAt.setRequiredIndicatorVisible(true);
        effectiveAt.setValue(LocalDateTime.now(ZoneId.of("Asia/Manila")));
        TextArea description = FormInputs.notes(new TextArea("Reported Information"));
        description.setRequiredIndicatorVisible(true);
        description.setWidthFull();
        Button save = new Button("Record", event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

            try {
                if (effectiveAt.getValue() == null || entityType.getValue() == null) {
                    throw new com.transit.arctransit.common.exception.CommandValidationException("Affected record and effective time are required.");
                }
                reportService.submitReport(new CreateOperationalReportCommand(source.getValue(), entityType.getValue(),
                        affectedRecord.getValue().id(),
                        effectiveAt.getValue().atZone(ZoneId.of("Asia/Manila")).toInstant(), description.getValue()));
                dialog.close();
                refreshGrid();
            } catch (com.transit.arctransit.common.exception.CommandValidationException
                    | com.transit.arctransit.common.exception.ResourceNotFoundException
                    | jakarta.validation.ConstraintViolationException | IllegalArgumentException exception) {
                showError(com.transit.arctransit.common.ui.ApplicationErrors.message(exception));
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        description.setHelperText("Example: The driver of ARC-001 reported a flat tire by radio near the terminal.");
        dialog.add(new VerticalLayout(source, entityType, affectedRecord, effectiveAt, description));
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), save);
        dialog.open();
    }

    private void openVerificationDialog(Long id) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Review Report " + id);
        ComboBox<String> status = FormInputs.choice(new ComboBox<>("Review Decision"));
        status.setRequiredIndicatorVisible(true);
        status.setItems("CONFIRMED", "DISPUTED", "REJECTED");
        TextArea reason = FormInputs.notes(new TextArea("Evidence and Reason"));
        reason.setRequiredIndicatorVisible(true);
        dialog.add(new VerticalLayout(status, reason));
        dialog.getFooter().add(new Button("Cancel", event -> dialog.close()), new Button("Save Review", event -> {
                            if (!FormInputs.validate(dialog)) {
                                return;
                            }

            try {
                reportService.verifyReport(id, status.getValue(), reason.getValue());
                dialog.close();
                refreshGrid();
            } catch (com.transit.arctransit.common.exception.CommandValidationException exception) {
                showError(com.transit.arctransit.common.ui.ApplicationErrors.message(exception));
            }
        }));
        dialog.open();
    }

    private void openHistory(Long id) {
        Dialog dialog = Dialogs.create();
        dialog.setHeaderTitle("Report Verification History");
        VerticalLayout entries = new VerticalLayout();
        for (var event : reportService.getVerificationHistory(id)) {
            entries.add(new Paragraph(event.recordedAt() + " | " + event.recordedBy() + " | "
                    + event.newStatus() + " | " + event.reason()));
        }
        dialog.add(entries);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }

    private static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SYSTEM_ADMIN".equals(authority.getAuthority()));
    }

    private static void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void openDetails(OperationalReportView report) {
        Dialog dialog = Dialogs.details("Operational Report Details");
        Dialogs.field(dialog, "Report number", report.id());
        Dialogs.field(dialog, "Reported information", report.description());
        Dialogs.field(dialog, "Source", report.sourceType());
        Dialogs.field(dialog, "Recorded by", report.reportedBy());
        Dialogs.field(dialog, "Reported at", report.reportedAt());
        Dialogs.field(dialog, "Effective at", report.effectiveAt());
        Dialogs.field(dialog, "Affected record type", report.entityType());
        Dialogs.field(dialog, "Affected record", recordLabel(report));
        Dialogs.field(dialog, "Information age", report.freshness());
        Dialogs.field(dialog, "Verification", report.verificationStatus());
        Dialogs.field(dialog, "Reviewed by", report.verifiedBy());
        Dialogs.field(dialog, "Reviewed at", report.verifiedAt());
        Dialogs.field(dialog, "Evidence and reason", report.verificationReason());
        dialog.open();
    }

    private String recordLabel(OperationalReportView report) {
        return recordLabels.computeIfAbsent(report.entityType() + ":" + report.entityId(), key -> {
            try {
                return switch (report.entityType()) {
                    case "FLEET" -> fleetService.getUnit(report.entityId()).unitNumber();
                    case "DRIVER" -> {
                        var driver = driverService.getDriver(report.entityId());
                        yield driver.employeeNumber() + " - " + driver.firstName() + " " + driver.lastName();
                    }
                    case "ROUTE" -> {
                        var route = routeService.getRoute(report.entityId());
                        yield route.routeCode() + " - " + route.routeName();
                    }
                    default -> {
                        var trip = dispatchService.getAssignment(report.entityId());
                        yield "Dispatch #" + trip.id() + " - " + trip.fleetUnitNumber() + " - " + trip.routeCode();
                    }
                };
            } catch (com.transit.arctransit.common.exception.ResourceNotFoundException exception) {
                return report.entityType() + " #" + report.entityId() + " (no longer available)";
            }
        });
    }
}
