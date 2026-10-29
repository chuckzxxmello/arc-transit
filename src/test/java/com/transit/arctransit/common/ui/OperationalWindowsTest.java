package com.transit.arctransit.common.ui;

import com.transit.arctransit.analytics.ui.ArchiveView;
import com.transit.arctransit.audit.*;
import com.transit.arctransit.audit.ui.AuditHistoryView;
import com.transit.arctransit.auth.*;
import com.transit.arctransit.dispatch.*;
import com.transit.arctransit.dispatch.ui.DispatchView;
import com.transit.arctransit.driver.*;
import com.transit.arctransit.driver.ui.DriverManagementView;
import com.transit.arctransit.fleet.*;
import com.transit.arctransit.fleet.ui.FleetManagementView;
import com.transit.arctransit.incident.*;
import com.transit.arctransit.incident.ui.IncidentManagementView;
import com.transit.arctransit.maintenance.*;
import com.transit.arctransit.maintenance.ui.MaintenanceManagementView;
import com.transit.arctransit.reconciliation.*;
import com.transit.arctransit.reconciliation.ui.ExceptionQueueView;
import com.transit.arctransit.reporting.*;
import com.transit.arctransit.reporting.ui.OperationalReportingView;
import com.transit.arctransit.route.*;
import com.transit.arctransit.route.ui.RouteManagementView;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationalWindowsTest {
    private UI ui;
    private final Instant now = Instant.parse("2026-10-17T08:00:00Z");
    private FleetManagementService fleet;
    private AuthenticationContext authentication;

    @BeforeEach
    void setup() {
        VaadinSession session = mock(VaadinSession.class, RETURNS_DEEP_STUBS);
        when(session.hasLock()).thenReturn(true);
        VaadinSession.setCurrent(session);
        ui = new UI();
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
        authentication = mock(AuthenticationContext.class);
        when(authentication.hasRole(anyString())).thenReturn(true);
        fleet = stub(FleetManagementService.class);
    }

    @AfterEach
    void cleanup() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void maintenanceSearchSelectsBusIdentityRatherThanGuessingItsNumericId() {
        MaintenanceService maintenance = stub(MaintenanceService.class);
        when(fleet.searchUnits(any(), any())).thenReturn(new PageImpl<>(List.of(bus())));
        ui.add(new MaintenanceManagementView(maintenance, fleet));
        button(ui, "Request Maintenance").click();
        Dialog dialog = opened();
        ComboBox<RecordPicker.Option> picker = choice(dialog, "Bus");
        @SuppressWarnings("unchecked")
        var provider = (com.vaadin.flow.data.provider.DataProvider<RecordPicker.Option, String>) picker.getDataProvider();
        List<RecordPicker.Option> matches = provider.fetch(new Query<>(0, 50, List.of(), null, "ARC-901")).toList();
        assertEquals(List.of(new RecordPicker.Option(42L, "ARC-901 (TST-0901)")), matches);
        assertFalse(picker.isAllowCustomValue());
        picker.setValue(matches.getFirst());
        area(dialog, "Description").setValue("Inspect the door actuator.");
        button(dialog, "Request").click();
        ArgumentCaptor<CreateMaintenanceCommand> command = ArgumentCaptor.forClass(CreateMaintenanceCommand.class);
        verify(maintenance).requestMaintenance(command.capture());
        assertEquals(42L, command.getValue().fleetUnitId());
        assertFalse(dialog.isOpened());
    }

    @Test
    void incidentRequiresASelectedBusAndClearsItWhenAffectedTypeChanges() {
        IncidentManagementService incidents = stub(IncidentManagementService.class);
        ui.add(new IncidentManagementView(incidents, fleet));
        button(ui, "Report Incident").click();
        Dialog dialog = opened();
        text(dialog, "Title").setValue("Rear door reported stuck");
        ComboBox<String> type = choice(dialog, "Affected Service");
        type.setValue("FLEET");
        ComboBox<RecordPicker.Option> bus = choice(dialog, "Affected Bus");
        clearInvocations(incidents);
        button(dialog, "Report").click();
        assertTrue(dialog.isOpened());
        assertTrue(bus.isInvalid());
        verifyNoInteractions(incidents);
        bus.setValue(new RecordPicker.Option(42L, "ARC-901 (TST-0901)"));
        type.setValue("DRIVER");
        assertNull(bus.getValue());
        assertFalse(bus.isVisible());
        type.setValue("FLEET");
        assertNull(bus.getValue());
        bus.setValue(new RecordPicker.Option(42L, "ARC-901 (TST-0901)"));
        button(dialog, "Report").click();
        ArgumentCaptor<CreateIncidentCommand> command = ArgumentCaptor.forClass(CreateIncidentCommand.class);
        verify(incidents).reportIncident(command.capture());
        assertEquals(42L, command.getValue().affectedEntityId());
        assertEquals("FLEET", command.getValue().affectedServiceType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "edit"})
    void capacityAboveEightyIsBlockedInBothForms(String mode) throws Exception {
        when(fleet.getUnit(42L)).thenReturn(new FleetUnitView(42L, "ARC-901", "TST-0901", "BUS", (short) 40, "INACTIVE"));
        FleetManagementView view = new FleetManagementView(fleet, authentication);
        ui.add(view);
        if ("create".equals(mode)) {
            button(ui, "Add Bus").click();
        } else {
            invoke(view, "openEditDialog", new Class<?>[]{Long.class}, 42L);
        }
        Dialog dialog = opened();
        text(dialog, "Unit Number").setValue("ARC-901");
        text(dialog, "Plate Number").setValue("TST-0901");
        IntegerField capacity = descendants(dialog).filter(IntegerField.class::isInstance).map(IntegerField.class::cast)
                .findFirst().orElseThrow();
        assertEquals(80, capacity.getMax());
        clearInvocations(fleet);
        capacity.setValue(600);
        button(dialog, "Save").click();
        assertTrue(dialog.isOpened());
        assertTrue(capacity.isInvalid());
        verifyNoInteractions(fleet);
        capacity.setValue(80);
        button(dialog, "Save").click();
        assertFalse(dialog.isOpened());
        if ("create".equals(mode)) {
            verify(fleet).createUnit(new CreateFleetUnitCommand("ARC-901", "TST-0901", (short) 80));
        } else {
            verify(fleet).updateUnit(new UpdateFleetUnitCommand(42L, "ARC-901", "TST-0901", (short) 80));
        }
    }

    @Test
    void oneStatusSaveSubmitsBothSeparateValuesAndRequiresReasonForAvailabilityChange() throws Exception {
        when(fleet.getUnit(42L)).thenReturn(new FleetUnitView(42L, "ARC-901", "TST-0901", "BUS", (short) 40, "INACTIVE"));
        FleetManagementView view = new FleetManagementView(fleet, authentication);
        ui.add(view);
        invoke(view, "openStatusDialog", new Class<?>[]{Long.class, String.class}, 42L, "INACTIVE");
        Dialog dialog = opened();
        choice(dialog, "Administrative Status").setValue("ACTIVE");
        choice(dialog, "Reported Availability").setValue("AVAILABLE");
        clearInvocations(fleet);
        button(dialog, "Save").click();
        assertTrue(dialog.isOpened());
        verifyNoInteractions(fleet);
        area(dialog, "Reason for Availability Change").setValue("Staff reported availability after inspection.");
        button(dialog, "Save").click();
        verify(fleet).updateState(new UpdateFleetStateCommand(42L, "ACTIVE", "AVAILABLE",
                "Staff reported availability after inspection."));
        verifyNoMoreInteractions(fleet);
        assertFalse(dialog.isOpened());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fleet", "driver", "route", "dispatch", "incident", "maintenance", "report", "exception", "audit",
            "archive-bus", "archive-driver", "archive-route", "archive-dispatch", "archive-user", "archive-maintenance", "archive-incident", "archive-report"})
    void doubleClickOpensDraggableDetailsWithoutOperationalSidebars(String module) throws Exception {
        DriverManagementService drivers = stub(DriverManagementService.class);
        RouteManagementService routes = stub(RouteManagementService.class);
        DispatchService dispatch = stub(DispatchService.class);
        Object item;
        Component view;
        String gridField = "grid";
        DriverSummaryView driver = new DriverSummaryView(7L, "EMP-T901", "Carlo Reyes", "LICENSE-901", "2030-12-31", "ACTIVE", false);
        RouteSummaryView route = new RouteSummaryView(9L, "R901", "Riverside", 3, 45, "ACTIVE");
        DispatchAssignmentView trip = new DispatchAssignmentView(11L, "2026-10-18", 42L, "ARC-901", 7L,
                "Carlo Reyes", 9L, "R901", now.toString(), "SCHEDULED", 1L, now.plusSeconds(2700).toString(),
                null, null, "2026-10-17", null, false, null);
        switch (module) {
            case "fleet" -> { view = new FleetManagementView(fleet, authentication); item = bus(); }
            case "driver" -> { view = new DriverManagementView(drivers); item = driver; }
            case "route" -> { view = new RouteManagementView(routes, authentication); item = route; }
            case "dispatch" -> { view = new DispatchView(dispatch, fleet, drivers, routes); item = trip; }
            case "incident" -> {
                view = new IncidentManagementView(stub(IncidentManagementService.class), fleet);
                item = new IncidentView(1L, "Door issue", "Full incident text", "MEDIUM", "REPORTED", "FLEET", 42L,
                        "staff", now, null, null, null, now, "STAFF_ENTRY", null, null);
            }
            case "maintenance" -> {
                view = new MaintenanceManagementView(stub(MaintenanceService.class), fleet);
                item = new MaintenanceRecordView(2L, 42L, "CORRECTIVE", "REQUESTED", "Inspect door", null, null, null, null, now);
            }
            case "report" -> {
                when(fleet.getUnit(42L)).thenReturn(new FleetUnitView(42L, "ARC-901", "TST-0901", "BUS", (short) 40, "ACTIVE"));
                view = new OperationalReportingView(stub(OperationalReportService.class), fleet, stub(DriverManagementService.class), stub(RouteManagementService.class), stub(DispatchService.class));
                item = new OperationalReportView(3L, "PHONE", "staff", now, now, "UNVERIFIED", null, null,
                        "FLEET", 42L, "Full report text", "CURRENT", null, null);
            }
            case "exception" -> {
                ReconciliationService reconciliation = stub(ReconciliationService.class);
                when(reconciliation.getExceptionSummary()).thenReturn(new ExceptionSummaryView(0, java.util.Map.of(), java.util.Map.of()));
                view = new ExceptionQueueView(reconciliation, mock(org.springframework.beans.factory.ObjectProvider.class));
                item = new OperationalExceptionView(4L, "CONTRADICTION", "HIGH", "FLEET", 42L, "Conflict details", "OPEN",
                        now, null, null, null, null, null);
            }
            case "audit" -> {
                view = new AuditHistoryView(stub(AuditQueryService.class));
                item = new AuditEventView(5L, "FLEET_UNIT_UPDATED", "FleetUnit", 42L, "staff", now, "SUCCESS", "Full audit text ".repeat(100));
            }
            default -> {
                view = new ArchiveView(fleet, drivers, routes, dispatch, stub(UserAdministrationService.class), stub(IncidentManagementService.class), stub(MaintenanceService.class), stub(OperationalReportService.class));
                switch (module) {
                    case "archive-bus" -> { gridField = "fleetGrid"; item = bus(); }
                    case "archive-driver" -> { gridField = "driverGrid"; item = driver; }
                    case "archive-route" -> { gridField = "routeGrid"; item = route; }
                    case "archive-dispatch" -> { gridField = "dispatchGrid"; item = trip; }
                    case "archive-user" -> { gridField = "userGrid"; item = new UserSummaryView("sample.staff", "Sample Staff", "DISABLED"); }
                    case "archive-maintenance" -> {
                        gridField = "maintenanceGrid";
                        when(fleet.getUnit(42L)).thenReturn(new FleetUnitView(42L, "ARC-901", "TST-0901", "BUS", (short) 40, "ACTIVE"));
                        item = new MaintenanceRecordView(2L, 42L, "CORRECTIVE", "COMPLETED", "Inspect door", null, now, now, "Repair completed", now);
                    }
                    case "archive-incident" -> {
                        gridField = "incidentGrid";
                        item = new IncidentView(1L, "Door issue", "Incident details", "MEDIUM", "CLOSED", "FLEET", 42L,
                                "staff", now, now, now, "Resolved door issue", now, "STAFF_ENTRY", null, null);
                    }
                    case "archive-report" -> {
                        gridField = "reportGrid";
                        when(fleet.getUnit(42L)).thenReturn(new FleetUnitView(42L, "ARC-901", "TST-0901", "BUS", (short) 40, "ACTIVE"));
                        item = new OperationalReportView(3L, "PHONE", "staff", now, now, "UNVERIFIED", null, null,
                                "FLEET", 42L, "Full report text", "CURRENT", null, now);
                    }
                    default -> throw new IllegalArgumentException(module);
                }
            }
        }
        ui.add(view);
        assertFalse(descendants(view).anyMatch(SplitLayout.class::isInstance));
        Grid<Object> grid;
        if ("audit".equals(module)) {
            @SuppressWarnings("unchecked") Grid<Object> auditGrid = (Grid<Object>) descendants(view)
                    .filter(Grid.class::isInstance).findFirst().orElseThrow();
            grid = auditGrid;
        } else {
            var field = view.getClass().getDeclaredField(gridField);
            field.setAccessible(true);
            @SuppressWarnings("unchecked") Grid<Object> recordGrid = (Grid<Object>) field.get(view);
            grid = recordGrid;
        }
        ComponentUtil.fireEvent(grid, new AttachEvent(grid, false));
        grid.getColumns().forEach(column -> {
            assertTrue(column.isResizable());
            assertEquals(0, column.getFlexGrow());
        });
        String key = grid.getDataCommunicator().getKeyMapper().key(item);
        ComponentUtil.fireEvent(grid, new ItemDoubleClickEvent<>(grid, true, key, null, 0, 2,
                0, 0, 0, 0, false, false, false, false));
        Dialog dialog = opened();
        assertTrue(dialog.isDraggable());
        assertTrue(dialog.isResizable());
        assertTrue(descendants(dialog).anyMatch(Paragraph.class::isInstance));
        if ("audit".equals(module)) {
            assertTrue(descendants(dialog).filter(Paragraph.class::isInstance).map(Paragraph.class::cast)
                    .anyMatch(p -> p.getText().contains("Full audit text ".repeat(100))));
        }
        button(dialog, "Close").click();
        assertFalse(dialog.isOpened());
        ComponentUtil.fireEvent(grid, new ItemDoubleClickEvent<>(grid, true, key, null, 0, 2,
                0, 0, 0, 0, false, false, false, false));
        assertTrue(opened().isOpened());
    }

    @Test
    void confirmationWindowsAreDraggableAndCancelDoesNotRunTheAction() {
        ConfirmationDialog dialog = new ConfirmationDialog();
        Runnable action = mock(Runnable.class);
        dialog.addConfirmListener(event -> action.run());
        dialog.open();
        assertTrue(dialog.isDraggable());
        button(dialog, "Cancel").click();
        verifyNoInteractions(action);
        dialog.open();
        button(dialog, "Confirm").click();
        verify(action).run();
        assertFalse(dialog.isOpened());
    }

    @Test
    void driverRegistrationRejectsInternationalOrIncompleteContactAndAcceptsLocalMobile() {
        DriverManagementService drivers = stub(DriverManagementService.class);
        ui.add(new DriverManagementView(drivers));
        button(ui, "Register Driver").click();
        Dialog dialog = opened();
        text(dialog, "Employee Number").setValue("EMP-901");
        text(dialog, "First Name").setValue("Carlos");
        text(dialog, "Last Name").setValue("Reyes");
        text(dialog, "License Number").setValue("LIC-901");
        descendants(dialog).filter(com.vaadin.flow.component.datepicker.DatePicker.class::isInstance)
                .map(com.vaadin.flow.component.datepicker.DatePicker.class::cast).findFirst().orElseThrow()
                .setValue(java.time.LocalDate.now().plusYears(1));
        TextField contact = text(dialog, "Contact Number");
        assertEquals(11, contact.getMaxLength());
        clearInvocations(drivers);
        for (String value : List.of("+639691477344", "0969147734", "19691477344", "letters")) {
            contact.setValue(value);
            button(dialog, "Save").click();
            assertTrue(contact.isInvalid());
            assertTrue(dialog.isOpened());
            verifyNoInteractions(drivers);
        }
        contact.setValue("09691477344");
        button(dialog, "Save").click();
        var command = ArgumentCaptor.forClass(CreateDriverCommand.class);
        verify(drivers).registerDriver(command.capture());
        assertEquals("09691477344", command.getValue().contactNumber());
        assertFalse(dialog.isOpened());
    }

    @Test
    void driverArchiveIsVisibleAndRequiresConfirmation() throws Exception {
        DriverManagementService drivers = stub(DriverManagementService.class);
        DriverManagementView view = new DriverManagementView(drivers);
        ui.add(view);
        var driver = new DriverSummaryView(7L, "EMP-901", "Carlos Reyes", "LIC-901", "2030-12-31", "ACTIVE", false);
        Component actions = renderLastColumn(view, "grid", driver);
        Button archive = button(actions, "Archive");
        assertTrue(archive.isVisible());
        archive.click();
        button(opened(), "Cancel").click();
        verify(drivers, never()).archiveDriver(anyLong());
        archive.click();
        button(opened(), "Archive Driver").click();
        verify(drivers).archiveDriver(7L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dispatch", "user"})
    void archivedDispatchAndUserOfferConfirmedPermanentDeletion(String type) throws Exception {
        DispatchService dispatch = stub(DispatchService.class);
        UserAdministrationService users = stub(UserAdministrationService.class);
        ArchiveView view = new ArchiveView(fleet, stub(DriverManagementService.class), stub(RouteManagementService.class), dispatch, users, stub(IncidentManagementService.class), stub(MaintenanceService.class), stub(OperationalReportService.class));
        ui.add(view);
        Object row = "user".equals(type) ? new UserSummaryView("sample.staff", "Sample Staff", "DISABLED")
                : new DispatchAssignmentView(11L, "2026-10-18", 42L, "ARC-901", 7L, "Carlos Reyes", 9L, "R901", now.toString(), "CANCELLED");
        Component actions = renderLastColumn(view, "user".equals(type) ? "userGrid" : "dispatchGrid", row);
        Button delete = descendants(actions).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.getElement().getAttribute("theme").contains("error")).findFirst().orElseThrow();
        delete.click();
        button(opened(), "Cancel").click();
        verify(users, never()).hardDeleteUser(anyString());
        verify(dispatch, never()).hardDeleteAssignment(anyLong());
        delete.click();
        button(opened(), "Delete Permanently").click();
        if ("user".equals(type)) verify(users).hardDeleteUser("sample.staff");
        else verify(dispatch).hardDeleteAssignment(11L);
    }

    @Test
    void reportsArchiveOnlyAfterConfirmationAndStaffCannotSeeTheAction() throws Exception {
        OperationalReportService reports = stub(OperationalReportService.class);
        OperationalReportingView view = new OperationalReportingView(reports, fleet,
                stub(DriverManagementService.class), stub(RouteManagementService.class), stub(DispatchService.class));
        ui.add(view);
        var row = new OperationalReportView(3L, "PHONE", "staff", now, now, "UNVERIFIED", null, null,
                "FLEET", 42L, "Full report text", "CURRENT", null, null);
        Button archive = button(renderLastColumn(view, "grid", row), "Archive");
        archive.click();
        button(opened(), "Cancel").click();
        verify(reports, never()).archiveReport(anyLong());
        archive.click();
        button(opened(), "Archive Report").click();
        verify(reports).archiveReport(3L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "staff", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATIONS_STAFF"))));
        assertFalse(descendants(renderLastColumn(view, "grid", row)).filter(Button.class::isInstance)
                .map(Button.class::cast).anyMatch(button -> "Archive".equals(button.getText())));
    }

    @Test
    void archivedReportRestoresOrDeletesOnlyAfterConfirmation() throws Exception {
        OperationalReportService reports = stub(OperationalReportService.class);
        ArchiveView view = new ArchiveView(fleet, stub(DriverManagementService.class), stub(RouteManagementService.class),
                stub(DispatchService.class), stub(UserAdministrationService.class), stub(IncidentManagementService.class),
                stub(MaintenanceService.class), reports);
        ui.add(view);
        var row = new OperationalReportView(3L, "RADIO", "staff", now, now, "CONFIRMED", "admin", now,
                "FLEET", 42L, "Reported information", "CURRENT", "Inspection evidence", now);
        Component actions = renderLastColumn(view, "reportGrid", row);
        button(actions, "Delete Permanently").click();
        button(opened(), "Cancel").click();
        verify(reports, never()).hardDeleteReport(anyLong());
        button(actions, "Restore").click();
        verify(reports).restoreReport(3L);
        button(actions, "Delete Permanently").click();
        button(opened(), "Delete Permanently").click();
        verify(reports).hardDeleteReport(3L);
    }

    @Test
    void unlinkWindowValidatesReasonAndSubmitsExpectedCurrentLink() throws Exception {
        IncidentManagementService incidents = stub(IncidentManagementService.class);
        IncidentManagementView view = new IncidentManagementView(incidents, fleet);
        ui.add(view);
        var incident = new IncidentView(1L, "Door", "Reported", "LOW", "REPORTED", "FLEET", 42L,
                "staff", now, null, null, null, now, "STAFF_ENTRY", null, 9L);
        invoke(view, "openUnlinkMaintenanceDialog", new Class<?>[]{IncidentView.class}, incident);
        Dialog dialog = opened();
        clearInvocations(incidents);
        button(dialog, "Unlink").click();
        assertTrue(area(dialog, "Reason for Unlinking").isInvalid());
        verifyNoInteractions(incidents);
        area(dialog, "Reason for Unlinking").setValue("Wrong repair record selected.");
        button(dialog, "Unlink").click();
        verify(incidents).unlinkMaintenanceRecord(1L, 9L, "Wrong repair record selected.");
        assertFalse(dialog.isOpened());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fleet", "driver", "dispatch", "route", "incident", "maintenance", "report"})
    void refreshControlInvalidatesDisplayedDataWithoutSavingRecords(String module) {
        Component view = switch (module) {
            case "fleet" -> new FleetManagementView(fleet, authentication);
            case "driver" -> new DriverManagementView(stub(DriverManagementService.class));
            case "dispatch" -> new DispatchView(stub(DispatchService.class), fleet, stub(DriverManagementService.class), stub(RouteManagementService.class));
            case "route" -> new RouteManagementView(stub(RouteManagementService.class), authentication);
            case "incident" -> new IncidentManagementView(stub(IncidentManagementService.class), fleet);
            case "maintenance" -> new MaintenanceManagementView(stub(MaintenanceService.class), fleet);
            default -> new OperationalReportingView(stub(OperationalReportService.class), fleet, stub(DriverManagementService.class), stub(RouteManagementService.class), stub(DispatchService.class));
        };
        ui.add(view);
        Grid<?> grid = descendants(view).filter(Grid.class::isInstance).map(Grid.class::cast).findFirst().orElseThrow();
        java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
        var originalProvider = grid.getDataProvider();
        grid.getDataProvider().addDataProviderListener(event -> changes.incrementAndGet());
        RefreshButton refresh = descendants(view).filter(RefreshButton.class::isInstance).map(RefreshButton.class::cast)
                .findFirst().orElseThrow();
        refresh.click();
        assertTrue(changes.get() > 0 || grid.getDataProvider() != originalProvider);
        assertTrue(refresh.isEnabled());
    }

    @Test
    void sortableNavigationKeepsAuthorizedLinksAndSavesOnlyKnownUniquePaths() throws Exception {
        var nav = new com.vaadin.flow.component.sidenav.SideNav();
        nav.addItem(new com.vaadin.flow.component.sidenav.SideNavItem("Dashboard Overview", ""),
                new com.vaadin.flow.component.sidenav.SideNavItem("Dispatch", "dispatch"),
                new com.vaadin.flow.component.sidenav.SideNavItem("Fleet", "fleet"));
        ModuleNavigation modules = new ModuleNavigation(nav, "sample.staff");
        ui.add(modules);
        modules.applyOrder(List.of("fleet", "fleet", "admin/users", "dispatch"));
        assertEquals(List.of("fleet", "dispatch", ""), modules.currentOrder());
        modules.saveOrder("dispatch,admin/users,fleet,fleet,");
        assertEquals(List.of("dispatch", "fleet", ""), modules.currentOrder());
        modules.saveOrder(null);
        modules.saveOrder("x".repeat(2001));
        assertEquals(List.of("dispatch", "fleet", ""), modules.currentOrder());
        assertEquals(3, nav.getItems().size());
        nav.getItems().forEach(item -> {
            assertEquals(item.getPath(), item.getElement().getAttribute("data-module-path"));
            assertEquals("Reorder " + item.getLabel(),
                    item.getSuffixComponent().getElement().getAttribute("aria-label"));
        });
        assertFalse(descendants(modules).filter(Button.class::isInstance).map(Button.class::cast)
                .anyMatch(b -> b.getText().contains("Organize")));
        var sortable = descendants(modules).filter(org.vaadin.jchristophe.SortableLayout.class::isInstance)
                .map(org.vaadin.jchristophe.SortableLayout.class::cast).findFirst().orElseThrow();
        var reorder = org.vaadin.jchristophe.SortableLayout.class.getDeclaredMethod("onReorderListener",
                tools.jackson.databind.node.ArrayNode.class, tools.jackson.databind.node.ArrayNode.class);
        reorder.setAccessible(true);
        var factory = tools.jackson.databind.node.JsonNodeFactory.instance;
        reorder.invoke(sortable, factory.arrayNode().add(2), factory.arrayNode().add(0));
        assertEquals(List.of("", "dispatch", "fleet"), modules.currentOrder());
        reorder.invoke(sortable, factory.arrayNode().add(0), factory.arrayNode().add(2));
        assertEquals(List.of("dispatch", "fleet", ""), modules.currentOrder());
    }

    @Test
    void operationalReportsUseSearchableIdentityAndClearSelectionWhenTypeChanges() {
        OperationalReportService reports = stub(OperationalReportService.class);
        when(fleet.searchUnits(any(), any())).thenReturn(new PageImpl<>(List.of(bus())));
        OperationalReportingView view = new OperationalReportingView(reports, fleet,
                stub(DriverManagementService.class), stub(RouteManagementService.class), stub(DispatchService.class));
        ui.add(view);
        button(view, "Record Report").click();
        Dialog dialog = opened();
        ComboBox<String> type = choice(dialog, "Affected Record Type");
        ComboBox<RecordPicker.Option> record = choice(dialog, "Affected Record");
        assertFalse(record.isEnabled());
        type.setValue("FLEET");
        @SuppressWarnings("unchecked")
        var provider = (com.vaadin.flow.data.provider.DataProvider<RecordPicker.Option, String>) record.getDataProvider();
        var matches = provider.fetch(new Query<>(0, 20, List.of(), null, "ARC-901")).toList();
        assertEquals(List.of(new RecordPicker.Option(42L, "ARC-901 (TST-0901)")), matches);
        record.setValue(matches.getFirst());
        type.setValue("ROUTE");
        assertNull(record.getValue());
        area(dialog, "Reported Information").setValue("Driver reported a door issue by radio.");
        button(dialog, "Record").click();
        verify(reports, never()).submitReport(any());
        type.setValue("FLEET");
        record.setValue(matches.getFirst());
        button(dialog, "Record").click();
        ArgumentCaptor<CreateOperationalReportCommand> command = ArgumentCaptor.forClass(CreateOperationalReportCommand.class);
        verify(reports).submitReport(command.capture());
        assertEquals(42L, command.getValue().entityId());
        assertEquals("FLEET", command.getValue().entityType());
        assertEquals("STAFF_ENTRY", command.getValue().sourceType());
        assertFalse(dialog.isOpened());
    }

    @Test
    void dashboardMapLoadsLocalAssetsWithoutAKeyOrGoogleConfiguration() {
        OperationsReferenceMap map = new OperationsReferenceMap();
        assertEquals("arc-ui/map/index.html", map.getSrc());
        assertEquals("Cavite road map and weather reference", map.getElement().getAttribute("title"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Component renderLastColumn(Component view, String field, Object row) throws Exception {
        var property = view.getClass().getDeclaredField(field);
        property.setAccessible(true);
        Grid grid = (Grid) property.get(view);
        Grid.Column column = (Grid.Column) grid.getColumns().getLast();
        var renderer = (com.vaadin.flow.data.renderer.ComponentRenderer) column.getRenderer();
        return renderer.createComponent(row);
    }

    private FleetUnitSummaryView bus() {
        return new FleetUnitSummaryView(42L, "ARC-901", "TST-0901", (short) 40, "INACTIVE");
    }

    private Dialog opened() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return descendants(ui).filter(Dialog.class::isInstance).map(Dialog.class::cast)
                .filter(Dialog::isOpened).findFirst().orElseThrow();
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        var method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    private static <T> T stub(Class<T> type) {
        return mock(type, invocation -> invocation.getMethod().getReturnType() == Page.class
                ? Page.empty() : RETURNS_DEFAULTS.answer(invocation));
    }

    private static Stream<Component> descendants(Component root) {
        Stream<Component> children = root.getChildren();
        if (root instanceof Dialog dialog) {
            children = Stream.concat(children, dialog.getFooter().getElement().getChildren()
                    .flatMap(element -> element.getComponent().stream()));
        }
        return Stream.concat(Stream.of(root), children.flatMap(OperationalWindowsTest::descendants));
    }

    private static Button button(Component root, String label) {
        return descendants(root).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> label.equals(button.getText())).findFirst().orElseThrow();
    }

    private static TextField text(Component root, String label) {
        return descendants(root).filter(TextField.class::isInstance).map(TextField.class::cast)
                .filter(field -> label.equals(field.getLabel())).findFirst().orElseThrow();
    }

    private static TextArea area(Component root, String label) {
        return descendants(root).filter(TextArea.class::isInstance).map(TextArea.class::cast)
                .filter(field -> label.equals(field.getLabel())).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static <T> ComboBox<T> choice(Component root, String label) {
        return (ComboBox<T>) descendants(root).filter(ComboBox.class::isInstance).map(ComboBox.class::cast)
                .filter(field -> label.equals(field.getLabel())).findFirst().orElseThrow();
    }
}
