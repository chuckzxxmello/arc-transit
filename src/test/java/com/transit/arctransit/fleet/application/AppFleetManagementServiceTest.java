package com.transit.arctransit.fleet.application;

// ============================================================
// FLEET / APPLICATION CONTRACTS
// ============================================================

import com.transit.arctransit.fleet.CreateFleetUnitCommand;
import com.transit.arctransit.fleet.FleetUnitView;
import com.transit.arctransit.fleet.FleetUnitSummaryView;

// ============================================================
// FLEET DOMAIN
// ============================================================

import com.transit.arctransit.fleet.domain.FleetUnit;
import com.transit.arctransit.fleet.domain.FleetUnitRepository;
import com.transit.arctransit.fleet.domain.VehicleAvailabilityHistoryRepository;
import com.transit.arctransit.fleet.domain.OperationalStatus;
import com.transit.arctransit.fleet.domain.VehicleType;

// ============================================================
// COMMON EXCEPTIONS
// ============================================================

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// ============================================================
// AUDIT
// ============================================================

import com.transit.arctransit.audit.AuditRecordingService;

// ============================================================
// JUNIT
// ============================================================

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// ============================================================
// MOCKITO
// ============================================================

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// ============================================================
// SPRING DATA
// ============================================================

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

// ============================================================
// JAVA
// ============================================================

import java.util.List;
import java.util.Optional;

// ============================================================
// STATIC ASSERTIONS
// ============================================================

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// ============================================================
// STATIC MOCKITO
// ============================================================

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit tests for {@link AppFleetManagementService}.
@ExtendWith(MockitoExtension.class)
class AppFleetManagementServiceTest {

    // ============================================================
    // TEST DEPENDENCIES
    // ============================================================

    @Mock
    private FleetUnitRepository fleetUnitRepository;

    @Mock
    private AuditRecordingService auditService;

    @Mock
    private VehicleAvailabilityHistoryRepository historyRepository;

    @InjectMocks
    private AppFleetManagementService service;

    private FleetUnit existingUnit;

    // ============================================================
    // TEST FIXTURE
    // ============================================================

    @BeforeEach
    void setUp() {

        existingUnit = new FleetUnit(
                "BUS-001",
                "ABC-1234",
                VehicleType.BUS,
                (short) 50
        );

        /*
         * FleetUnit uses an identity-generated ID in production.
         *
         * The test assigns a deterministic ID so repository-based
         * service logic can be exercised without a real database.
         */
        setEntityId(existingUnit, 1L);
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Test
    void createUnit_success() {

        /*
         * CHANGED:
         *
         * The application service now uses existsBy...() instead
         * of findBy...().isPresent() for duplicate detection.
         */
        when(fleetUnitRepository.existsByUnitNumber("BUS-002"))
                .thenReturn(false);

        when(fleetUnitRepository.existsByPlateNumber("XYZ-5678"))
                .thenReturn(false);

        /*
         * The service uses save() and expects the returned entity.
         *
         * Returning the supplied entity is sufficient for a unit test
         * because the test does not require a database-generated ID.
         */
        when(fleetUnitRepository.save(any(FleetUnit.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        CreateFleetUnitCommand command =
                new CreateFleetUnitCommand(
                        "bus-002",
                        "xyz-5678",
                        (short) 40
                );

        FleetUnitView result =
                service.createUnit(command);

        assertNotNull(result);
        assertEquals("BUS-002", result.unitNumber());
        assertEquals("XYZ-5678", result.plateNumber());
        assertEquals("INACTIVE", result.operationalStatus());

        verify(fleetUnitRepository)
                .save(any(FleetUnit.class));
    }

    @Test
    void createUnit_duplicateUnitNumber_throwsConflict() {

        /*
         * CHANGED:
         *
         * The new service asks the repository whether the unit number
         * already exists.
         */
        when(fleetUnitRepository.existsByUnitNumber("BUS-001"))
                .thenReturn(true);

        CreateFleetUnitCommand command =
                new CreateFleetUnitCommand(
                        "bus-001",
                        "xyz-9999",
                        (short) 40
                );

        assertThrows(
                BusinessConflictException.class,
                () -> service.createUnit(command)
        );
    }

    @Test
    void createUnit_duplicatePlateNumber_throwsConflict() {

        when(fleetUnitRepository.existsByUnitNumber("BUS-099"))
                .thenReturn(false);

        /*
         * CHANGED:
         *
         * Duplicate plate detection now uses existsByPlateNumber().
         */
        when(fleetUnitRepository.existsByPlateNumber("ABC-1234"))
                .thenReturn(true);

        CreateFleetUnitCommand command =
                new CreateFleetUnitCommand(
                        "bus-099",
                        "abc-1234",
                        (short) 40
                );

        assertThrows(
                BusinessConflictException.class,
                () -> service.createUnit(command)
        );
    }

    // ============================================================
    // STATUS
    // ============================================================

    @Test
    void changeStatus_success() {

        when(fleetUnitRepository.findLockedById(1L))
                .thenReturn(Optional.of(existingUnit));

        FleetUnitView result =
                service.changeStatus(
                        1L,
                        "ACTIVE"
                );

        assertEquals(
                "ACTIVE",
                result.operationalStatus()
        );
    }

    @Test
    void changeStatus_invalidStatus_throwsValidation() {

        when(fleetUnitRepository.findLockedById(1L))
                .thenReturn(Optional.of(existingUnit));

        assertThrows(
                CommandValidationException.class,
                () -> service.changeStatus(
                        1L,
                        "FLYING"
                )
        );
    }

    @Test
    void changeStatus_notFound_throwsNotFound() {

        when(fleetUnitRepository.findLockedById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.changeStatus(
                        99L,
                        "ACTIVE"
                )
        );
    }

    // ============================================================
    // ARCHIVE
    // ============================================================

    @Test
    void archiveUnit_success() {

        when(fleetUnitRepository.findLockedById(1L))
                .thenReturn(Optional.of(existingUnit));

        service.archiveUnit(1L);

        assertNotNull(
                existingUnit.getArchivedAt()
        );
    }

    // ============================================================
    // UNARCHIVE
    // ============================================================

    @Test
    void unarchiveUnit_success() {

        existingUnit.archive();

        when(fleetUnitRepository.findLockedById(1L))
                .thenReturn(Optional.of(existingUnit));

        service.unarchiveUnit(1L);

        assertNull(
                existingUnit.getArchivedAt()
        );
    }

    // ============================================================
    // SEARCH
    // ============================================================

    @Test
    void searchUnits_returnsPagedResults() {

        Page<FleetUnit> page =
                new PageImpl<>(
                        List.of(existingUnit)
                );

        /*
         * CHANGED:
         *
         * The application service now calls searchActive()
         * instead of findByArchivedAtIsNull().
         *
         * query = null means no search filter.
         * status = null means no status filter.
         */
        when(fleetUnitRepository.searchActive(
                null,
                null,
                PageRequest.of(0, 10)
        )).thenReturn(page);

        Page<FleetUnitSummaryView> result =
                service.searchUnits(
                        null,
                        PageRequest.of(0, 10)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                "BUS-001",
                result.getContent()
                        .get(0)
                        .unitNumber()
        );
    }


    @Test
    void administrativeInactivityPreservesRecordedAvailability() {
        existingUnit.activate();
        when(fleetUnitRepository.findLockedById(1L)).thenReturn(Optional.of(existingUnit));

        FleetUnitView result = service.changeAdministrativeStatus(1L, "INACTIVE");

        assertEquals("INACTIVE", result.administrativeStatus());
        assertEquals("AVAILABLE", result.availability());
        org.junit.jupiter.api.Assertions.assertFalse(existingUnit.isAvailableForDispatch());
    }

    @Test
    void availabilityChangeRetainsActorReasonAndTimes() {
        when(fleetUnitRepository.findLockedById(1L)).thenReturn(Optional.of(existingUnit));
        java.time.Instant effectiveAt = java.time.Instant.now();
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "dispatcher", null, List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication);
        try {
            FleetUnitView result = service.changeAvailability(
                    new com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand(
                            1L, "INSPECTION_HOLD", effectiveAt, "Inspection requested"));

            assertEquals("INACTIVE", result.administrativeStatus());
            assertEquals("INSPECTION_HOLD", result.availability());
            var event = org.mockito.ArgumentCaptor.forClass(
                    com.transit.arctransit.fleet.domain.VehicleAvailabilityHistory.class);
            verify(historyRepository).save(event.capture());
            assertEquals("UNKNOWN", event.getValue().getPreviousAvailability());
            assertEquals("INSPECTION_HOLD", event.getValue().getAvailability());
            assertEquals("dispatcher", event.getValue().getRecordedBy());
            assertEquals("STAFF_ENTRY", event.getValue().getSourceType());
            assertEquals("UNVERIFIED", event.getValue().getVerificationStatus());
            assertEquals("Inspection requested", event.getValue().getReason());
            assertEquals(effectiveAt, event.getValue().getEffectiveAt());
            org.junit.jupiter.api.Assertions.assertFalse(
                    event.getValue().getRecordedAt().isBefore(effectiveAt));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void staleAvailabilityReportDoesNotChangeStateOrAppendHistory() {
        when(fleetUnitRepository.findLockedById(1L)).thenReturn(Optional.of(existingUnit));

        assertThrows(CommandValidationException.class, () -> service.changeAvailability(
                new com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand(
                        1L, "AVAILABLE", java.time.Instant.EPOCH, "Old report")));

        assertEquals("UNKNOWN", existingUnit.getAvailability().name());
        org.mockito.Mockito.verifyNoInteractions(historyRepository, auditService);
    }

    @Test
    void blankAvailabilityReasonIsRejectedBeforeMutation() {
        when(fleetUnitRepository.findLockedById(1L)).thenReturn(Optional.of(existingUnit));

        assertThrows(CommandValidationException.class, () -> service.changeAvailability(
                new com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand(
                        1L, "AVAILABLE", java.time.Instant.now(), " ")));

        assertEquals("UNKNOWN", existingUnit.getAvailability().name());
        org.mockito.Mockito.verifyNoInteractions(historyRepository, auditService);
    }

    @Test
    void archivedUnitCannotReceiveAvailabilityReports() {
        existingUnit.archive();
        when(fleetUnitRepository.findLockedById(1L)).thenReturn(Optional.of(existingUnit));

        assertThrows(CommandValidationException.class, () -> service.changeAvailability(
                new com.transit.arctransit.fleet.ChangeVehicleAvailabilityCommand(
                        1L, "AVAILABLE", java.time.Instant.now(), "Reported available")));

        org.mockito.Mockito.verifyNoInteractions(historyRepository, auditService);
    }

    // ============================================================
    // TEST UTILITIES
    // ============================================================

    /**
     * Assigns the generated entity ID for unit testing.
     *
     * <p>Production IDs are generated by PostgreSQL/JPA. The unit
     * tests do not require a running database, so a deterministic
     * ID is assigned directly.</p>
     */
    private static void setEntityId(
            FleetUnit fleetUnit,
            Long id) {

        try {

            var idField =
                    FleetUnit.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(fleetUnit, id);

        } catch (ReflectiveOperationException exception) {

            throw new IllegalStateException(
                    "Unable to initialize test fleet-unit ID.",
                    exception
            );
        }
    }
}