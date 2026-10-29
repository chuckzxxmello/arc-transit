package com.transit.arctransit.maintenance.application;

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand;
import com.transit.arctransit.fleet.FleetManagementService;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.maintenance.CreateMaintenanceCommand;
import com.transit.arctransit.maintenance.domain.MaintenanceHistory;
import com.transit.arctransit.maintenance.domain.MaintenanceHistoryRepository;
import com.transit.arctransit.maintenance.domain.MaintenanceRecord;
import com.transit.arctransit.maintenance.domain.MaintenanceRepository;
import com.transit.arctransit.maintenance.domain.MaintenanceStatus;
import com.transit.arctransit.maintenance.domain.MaintenanceType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppMaintenanceServiceTest {

    @Mock
    private MaintenanceRepository repository;

    @Mock
    private FleetManagementService fleetService;

    @Mock
    private AuditRecordingService auditService;

    @Mock
    private MaintenanceHistoryRepository historyRepository;

    @InjectMocks
    private AppMaintenanceService service;

    @Test
    void maintenanceRequestLocksFleetBeforeCreatingHoldAndHistory() {
        when(fleetService.lockUnitForAssignment(10L)).thenReturn(unit("AVAILABLE", null));
        when(repository.save(any(MaintenanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.requestMaintenance(new CreateMaintenanceCommand(
                10L, "CORRECTIVE", "Reported brake concern", null));

        assertEquals("REQUESTED", result.maintenanceStatus());
        var order = inOrder(fleetService, repository, historyRepository);
        order.verify(fleetService).lockUnitForAssignment(10L);
        order.verify(repository).save(any(MaintenanceRecord.class));
        order.verify(historyRepository).save(any(MaintenanceHistory.class));
    }

    @Test
    void archivedFleetCannotReceiveMaintenanceRequest() {
        when(fleetService.lockUnitForAssignment(10L)).thenReturn(unit("AVAILABLE", Instant.now()));

        assertThrows(CommandValidationException.class, () -> service.requestMaintenance(
                new CreateMaintenanceCommand(10L, "SCHEDULED", "Inspection", null)));

        verifyNoInteractions(repository, historyRepository, auditService);
    }

    @Test
    void maintenanceStartPreservesInspectionHold() {
        MaintenanceRecord record = record(MaintenanceStatus.APPROVED);
        lookup(record, "INSPECTION_HOLD");

        var result = service.startMaintenance(1L);

        assertEquals("IN_PROGRESS", result.maintenanceStatus());
        verify(fleetService, never()).changeAvailability(any());
        verify(historyRepository).save(any(MaintenanceHistory.class));
    }

    @Test
    void maintenanceStartRecordsMaintenanceWithoutChangingAdministrativeState() {
        MaintenanceRecord record = record(MaintenanceStatus.APPROVED);
        lookup(record, "AVAILABLE");

        service.startMaintenance(1L);

        var change = ArgumentCaptor.forClass(ChangeVehicleAvailabilityCommand.class);
        verify(fleetService).changeAvailability(change.capture());
        assertEquals("MAINTENANCE", change.getValue().availability());
        verify(fleetService, never()).changeAdministrativeStatus(any(), any());
        verify(fleetService, never()).changeStatus(any(), any());
    }

    @Test
    void lastMaintenanceCompletionRequiresNewAvailabilityReport() {
        lookup(record(MaintenanceStatus.IN_PROGRESS), "MAINTENANCE");
        when(repository.countByFleetUnitIdAndMaintenanceStatusNotIn(10L,
                List.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED))).thenReturn(0L);

        var result = service.completeMaintenance(1L, "Repair recorded");

        assertEquals("COMPLETED", result.maintenanceStatus());
        var change = ArgumentCaptor.forClass(ChangeVehicleAvailabilityCommand.class);
        verify(fleetService).changeAvailability(change.capture());
        assertEquals("UNKNOWN", change.getValue().availability());
        var history = ArgumentCaptor.forClass(MaintenanceHistory.class);
        verify(historyRepository).save(history.capture());
        assertEquals("IN_PROGRESS", history.getValue().getPreviousStatus());
        assertEquals("COMPLETED", history.getValue().getStatus());
        verify(fleetService, never()).changeStatus(any(), any());
    }

    @Test
    void maintenanceCompletionPreservesOtherActiveMaintenance() {
        lookup(record(MaintenanceStatus.IN_PROGRESS), "MAINTENANCE");
        when(repository.countByFleetUnitIdAndMaintenanceStatusNotIn(10L,
                List.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED))).thenReturn(1L);

        service.completeMaintenance(1L, "One repair completed");

        verify(fleetService, never()).changeAvailability(any());
    }

    @Test
    void maintenanceCompletionPreservesOutOfServiceAvailability() {
        lookup(record(MaintenanceStatus.IN_PROGRESS), "OUT_OF_SERVICE");

        service.completeMaintenance(1L, "Repair recorded; other concern remains");

        verify(fleetService, never()).changeAvailability(any());
    }

    @Test
    void maintenanceCompletionRejectsBlankNotesBeforeLifecycleMutation() {
        MaintenanceRecord record = record(MaintenanceStatus.IN_PROGRESS);
        lookup(record, "MAINTENANCE");

        assertThrows(CommandValidationException.class, () -> service.completeMaintenance(1L, " "));

        assertEquals(MaintenanceStatus.IN_PROGRESS, record.getMaintenanceStatus());
        verify(fleetService, never()).changeAvailability(any());
        verifyNoInteractions(historyRepository, auditService);
    }

    @Test
    void cancellationOfLastInProgressMaintenanceDoesNotLeaveMaintenanceAvailability() {
        lookup(record(MaintenanceStatus.IN_PROGRESS), "MAINTENANCE");
        when(repository.countByFleetUnitIdAndMaintenanceStatusNotIn(10L,
                List.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED))).thenReturn(0L);

        var result = service.cancelMaintenance(1L);

        assertEquals("CANCELLED", result.maintenanceStatus());
        var change = ArgumentCaptor.forClass(ChangeVehicleAvailabilityCommand.class);
        verify(fleetService).changeAvailability(change.capture());
        assertEquals("UNKNOWN", change.getValue().availability());
    }

    @Test
    void invalidLifecycleTransitionWritesNoHistory() {
        MaintenanceRecord record = record(MaintenanceStatus.REQUESTED);
        lookup(record, "AVAILABLE");

        assertThrows(IllegalStateException.class, () -> service.startMaintenance(1L));

        assertEquals(MaintenanceStatus.REQUESTED, record.getMaintenanceStatus());
        verify(fleetService, never()).changeAvailability(any());
        verifyNoInteractions(historyRepository, auditService);
    }

    private void lookup(MaintenanceRecord record, String availability) {
        when(repository.findById(1L)).thenReturn(Optional.of(record));
        when(fleetService.lockUnitForAssignment(10L)).thenReturn(unit(availability, null));
    }

    private static FleetUnitView unit(String availability, Instant archivedAt) {
        return new FleetUnitView(10L, "BUS-010", "ABC-010", "BUS", (short) 40,
                "ACTIVE", "ACTIVE", availability, archivedAt);
    }

    private static MaintenanceRecord record(MaintenanceStatus status) {
        MaintenanceRecord record = new MaintenanceRecord(
                10L, MaintenanceType.CORRECTIVE, "Reported concern", null);
        try {
            var id = MaintenanceRecord.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(record, 1L);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        if (status == MaintenanceStatus.APPROVED || status == MaintenanceStatus.IN_PROGRESS) {
            record.approve();
        }
        if (status == MaintenanceStatus.IN_PROGRESS) {
            record.startWork();
        }
        return record;
    }
}