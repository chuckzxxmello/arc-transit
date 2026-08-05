package com.transit.arctransit.fleet.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.fleet.*;
import com.transit.arctransit.fleet.domain.FleetUnit;
import com.transit.arctransit.fleet.domain.FleetUnitRepository;
import com.transit.arctransit.fleet.domain.OperationalStatus;
import com.transit.arctransit.fleet.domain.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppFleetManagementService.
 * Uses Mockito to isolate business logic from database dependencies.
 *
 * Source: https://site.mockito.org/ (Mockito framework)
 * Source: https://junit.org/junit5/docs/current/user-guide/ (JUnit 5)
 */
@ExtendWith(MockitoExtension.class)
class AppFleetManagementServiceTest {

    @Mock
    private FleetUnitRepository fleetUnitRepository;
    
    @Mock
    private com.transit.arctransit.audit.AuditRecordingService auditService;

    @InjectMocks
    private AppFleetManagementService service;

    private FleetUnit existingUnit;

    @BeforeEach
    void setUp() {
        existingUnit = new FleetUnit("BUS-001", "ABC-1234", VehicleType.BUS, (short) 50);
        // Use reflection to set the ID since it's auto-generated
        try {
            var idField = FleetUnit.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existingUnit, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createUnit_success() {
        when(fleetUnitRepository.findByUnitNumber("BUS-002")).thenReturn(Optional.empty());
        when(fleetUnitRepository.findByPlateNumber("XYZ-5678")).thenReturn(Optional.empty());
        when(fleetUnitRepository.save(any(FleetUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateFleetUnitCommand cmd = new CreateFleetUnitCommand("bus-002", "xyz-5678", (short) 40);
        FleetUnitView result = service.createUnit(cmd);

        assertNotNull(result);
        assertEquals("BUS-002", result.unitNumber());
        assertEquals("XYZ-5678", result.plateNumber());
        assertEquals("INACTIVE", result.operationalStatus());
        verify(fleetUnitRepository).save(any(FleetUnit.class));
    }

    @Test
    void createUnit_duplicateUnitNumber_throwsConflict() {
        when(fleetUnitRepository.findByUnitNumber("BUS-001")).thenReturn(Optional.of(existingUnit));

        CreateFleetUnitCommand cmd = new CreateFleetUnitCommand("bus-001", "xyz-9999", (short) 40);
        assertThrows(BusinessConflictException.class, () -> service.createUnit(cmd));
    }

    @Test
    void createUnit_duplicatePlateNumber_throwsConflict() {
        when(fleetUnitRepository.findByUnitNumber("BUS-099")).thenReturn(Optional.empty());
        when(fleetUnitRepository.findByPlateNumber("ABC-1234")).thenReturn(Optional.of(existingUnit));

        CreateFleetUnitCommand cmd = new CreateFleetUnitCommand("bus-099", "abc-1234", (short) 40);
        assertThrows(BusinessConflictException.class, () -> service.createUnit(cmd));
    }

    @Test
    void changeStatus_success() {
        when(fleetUnitRepository.findById(1L)).thenReturn(Optional.of(existingUnit));

        FleetUnitView result = service.changeStatus(1L, "ACTIVE");
        assertEquals("ACTIVE", result.operationalStatus());
    }

    @Test
    void changeStatus_invalidStatus_throwsValidation() {
        when(fleetUnitRepository.findById(1L)).thenReturn(Optional.of(existingUnit));

        assertThrows(CommandValidationException.class, () -> service.changeStatus(1L, "FLYING"));
    }

    @Test
    void changeStatus_notFound_throwsNotFound() {
        when(fleetUnitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.changeStatus(99L, "ACTIVE"));
    }

    @Test
    void archiveUnit_success() {
        when(fleetUnitRepository.findById(1L)).thenReturn(Optional.of(existingUnit));

        service.archiveUnit(1L);
        assertNotNull(existingUnit.getArchivedAt());
    }

    @Test
    void unarchiveUnit_success() {
        existingUnit.archive();
        when(fleetUnitRepository.findById(1L)).thenReturn(Optional.of(existingUnit));

        service.unarchiveUnit(1L);
        assertNull(existingUnit.getArchivedAt());
    }

    @Test
    void searchUnits_returnsPagedResults() {
        Page<FleetUnit> page = new PageImpl<>(List.of(existingUnit));
        when(fleetUnitRepository.findByArchivedAtIsNull(any())).thenReturn(page);

        Page<FleetUnitSummaryView> result = service.searchUnits(null, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals("BUS-001", result.getContent().get(0).unitNumber());
    }
}
