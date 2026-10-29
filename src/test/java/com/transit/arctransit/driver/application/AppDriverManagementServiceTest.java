package com.transit.arctransit.driver.application;

// driver module

import com.transit.arctransit.driver.CreateDriverCommand;
import com.transit.arctransit.driver.DriverSummaryView;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.driver.UpdateDriverCommand;

// driver domain

import com.transit.arctransit.driver.domain.Driver;
import com.transit.arctransit.driver.domain.DriverRepository;
import com.transit.arctransit.driver.domain.DriverAvailabilityHistoryRepository;
import com.transit.arctransit.driver.domain.LicenseType;

// audit

import com.transit.arctransit.audit.AuditRecordingService;

// common exceptions

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// junit

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// mockito

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// spring data

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

// java

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// assertions

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// mockito

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AppDriverManagementService.
 *
 * Tests application-level driver management logic without
 * requiring a real database.
 */
@ExtendWith(MockitoExtension.class)
class AppDriverManagementServiceTest {

    // dependencies

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AuditRecordingService auditService;

    @Mock
    private DriverAvailabilityHistoryRepository historyRepository;

    @InjectMocks
    private AppDriverManagementService service;

    // test fixture

    private Driver existingDriver;

    @BeforeEach
    void setUp() {

        existingDriver = new Driver(
                "EMP-001",
                "Juan",
                "Dela Cruz",
                "LIC-001",
                LicenseType.PROFESSIONAL,
                LocalDate.now().plusYears(1),
                "09171234567"
        );

        // assign deterministic ID for unit testing

        setEntityId(existingDriver, 1L);
    }

    // register driver

    @Test
    void registerDriver_success() {

        when(driverRepository.findByEmployeeNumber("EMP-002"))
                .thenReturn(Optional.empty());

        when(driverRepository.findByLicenseNumber("LIC-002"))
                .thenReturn(Optional.empty());

        when(driverRepository.save(any(Driver.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        CreateDriverCommand command =
                new CreateDriverCommand(
                        "emp-002",
                        "Maria",
                        "Santos",
                        "LIC-002",
                        "PROFESSIONAL",
                        LocalDate.now().plusYears(2),
                        "09181234567"
                );

        DriverView result =
                service.registerDriver(command);

        assertNotNull(result);
        assertEquals(
                "EMP-002",
                result.employeeNumber()
        );
        assertEquals(
                "Maria",
                result.firstName()
        );
        assertEquals(
                "ACTIVE",
                result.employmentStatus()
        );
        assertFalse(
                result.licenseExpired()
        );

        verify(driverRepository)
                .save(any(Driver.class));
    }

    @Test
    void registerDriver_duplicateEmployeeNumber_throwsConflict() {

        when(driverRepository.findByEmployeeNumber("EMP-001"))
                .thenReturn(Optional.of(existingDriver));

        CreateDriverCommand command =
                new CreateDriverCommand(
                        "emp-001",
                        "Pedro",
                        "Santos",
                        "LIC-999",
                        "PROFESSIONAL",
                        LocalDate.now().plusYears(2),
                        null
                );

        assertThrows(
                BusinessConflictException.class,
                () -> service.registerDriver(command)
        );
    }

    @Test
    void registerDriver_duplicateLicenseNumber_throwsConflict() {

        when(driverRepository.findByEmployeeNumber("EMP-099"))
                .thenReturn(Optional.empty());

        when(driverRepository.findByLicenseNumber("LIC-001"))
                .thenReturn(Optional.of(existingDriver));

        CreateDriverCommand command =
                new CreateDriverCommand(
                        "emp-099",
                        "Pedro",
                        "Santos",
                        "LIC-001",
                        "PROFESSIONAL",
                        LocalDate.now().plusYears(2),
                        null
                );

        assertThrows(
                BusinessConflictException.class,
                () -> service.registerDriver(command)
        );
    }

    @Test
    void registerDriver_invalidLicenseType_throwsValidation() {

        when(driverRepository.findByEmployeeNumber("EMP-099"))
                .thenReturn(Optional.empty());

        when(driverRepository.findByLicenseNumber("LIC-099"))
                .thenReturn(Optional.empty());

        CreateDriverCommand command =
                new CreateDriverCommand(
                        "emp-099",
                        "Pedro",
                        "Santos",
                        "LIC-099",
                        "INVALID_TYPE",
                        LocalDate.now().plusYears(2),
                        null
                );

        assertThrows(
                CommandValidationException.class,
                () -> service.registerDriver(command)
        );
    }

    // employment status

    @Test
    void changeEmploymentStatus_success() {

        when(driverRepository.findLockedById(1L))
                .thenReturn(Optional.of(existingDriver));

        DriverView result =
                service.changeEmploymentStatus(
                        1L,
                        "SUSPENDED"
                );

        assertEquals(
                "SUSPENDED",
                result.employmentStatus()
        );
    }

    @Test
    void changeEmploymentStatus_notFound_throwsNotFound() {

        when(driverRepository.findLockedById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.changeEmploymentStatus(
                        99L,
                        "ACTIVE"
                )
        );
    }

    // archive

    @Test
    void archiveDriver_success() {

        when(driverRepository.findLockedById(1L))
                .thenReturn(Optional.of(existingDriver));

        service.archiveDriver(1L);

        assertNotNull(
                existingDriver.getArchivedAt()
        );
    }

    // search

    @Test
    void searchDrivers_returnsPagedResults() {

        Page<Driver> page =
                new PageImpl<>(
                        List.of(existingDriver)
                );

        when(driverRepository.searchActive(
                null, null, PageRequest.of(0, 10)
        )).thenReturn(page);

        Page<DriverSummaryView> result =
                service.searchDrivers(
                        null,
                        PageRequest.of(0, 10)
                );

        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                "EMP-001",
                result.getContent()
                        .get(0)
                        .employeeNumber()
        );
    }


    @Test
    void driverAvailabilityIsIndependentOfEmploymentAndQualification() {
        when(driverRepository.findLockedById(1L)).thenReturn(Optional.of(existingDriver));

        DriverView result = service.changeAvailability(
                new com.transit.arctransit.driver.ChangeDriverAvailabilityCommand(
                        1L, "UNAVAILABLE", java.time.Instant.now(), "Leave reported"));

        assertEquals("ACTIVE", result.employmentStatus());
        assertEquals("PROFESSIONAL", result.licenseType());
        assertEquals("UNAVAILABLE", result.availability());
        var event = org.mockito.ArgumentCaptor.forClass(
                com.transit.arctransit.driver.domain.DriverAvailabilityHistory.class);
        verify(historyRepository).save(event.capture());
        assertEquals("UNKNOWN", event.getValue().getPreviousAvailability());
        assertEquals("UNAVAILABLE", event.getValue().getAvailability());
        assertEquals("UNVERIFIED", event.getValue().getVerificationStatus());
        assertEquals("Leave reported", event.getValue().getReason());
    }

    @Test
    void driverAvailabilityRejectsFutureEffectiveTimeWithoutChangingState() {
        when(driverRepository.findLockedById(1L)).thenReturn(Optional.of(existingDriver));

        assertThrows(CommandValidationException.class, () -> service.changeAvailability(
                new com.transit.arctransit.driver.ChangeDriverAvailabilityCommand(
                        1L, "AVAILABLE", java.time.Instant.now().plusSeconds(3600), "Future shift")));

        assertEquals("UNKNOWN", existingDriver.getAvailability().name());
        org.mockito.Mockito.verifyNoInteractions(historyRepository, auditService);
    }

    @Test
    void driverSearchPassesNormalizedQueryAndStatusToRepository() {
        var pageable = PageRequest.of(2, 20);
        when(driverRepository.searchActive("Juan", com.transit.arctransit.driver.domain.EmploymentStatus.ACTIVE,
                pageable)).thenReturn(Page.empty(pageable));

        service.searchDrivers(new com.transit.arctransit.driver.DriverQuery(" Juan ", "active"), pageable);

        verify(driverRepository).searchActive("Juan",
                com.transit.arctransit.driver.domain.EmploymentStatus.ACTIVE, pageable);
    }

    // test utility

    private static void setEntityId(
            Driver driver,
            Long id) {

        try {

            var idField =
                    Driver.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(driver, id);

        } catch (ReflectiveOperationException exception) {

            throw new IllegalStateException(
                    "Unable to initialize test driver ID.",
                    exception
            );
        }
    }
}