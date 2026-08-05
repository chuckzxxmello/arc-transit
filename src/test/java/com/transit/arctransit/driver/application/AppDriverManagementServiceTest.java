package com.transit.arctransit.driver.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.driver.*;
import com.transit.arctransit.driver.domain.Driver;
import com.transit.arctransit.driver.domain.DriverRepository;
import com.transit.arctransit.driver.domain.LicenseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppDriverManagementService.
 *
 * Source: https://site.mockito.org/
 * Source: https://junit.org/junit5/docs/current/user-guide/
 */
@ExtendWith(MockitoExtension.class)
class AppDriverManagementServiceTest {

    @Mock
    private DriverRepository driverRepository;
    
    @Mock
    private com.transit.arctransit.audit.AuditRecordingService auditService;

    @InjectMocks
    private AppDriverManagementService service;

    private Driver existingDriver;

    @BeforeEach
    void setUp() {
        existingDriver = new Driver(
                "EMP-001", "Juan", "Dela Cruz",
                "LIC-001", LicenseType.PROFESSIONAL,
                LocalDate.now().plusYears(1), "09171234567"
        );
        try {
            var idField = Driver.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(existingDriver, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void registerDriver_success() {
        when(driverRepository.findByEmployeeNumber("EMP-002")).thenReturn(Optional.empty());
        when(driverRepository.findByLicenseNumber("LIC-002")).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDriverCommand cmd = new CreateDriverCommand(
                "emp-002", "Maria", "Santos", "LIC-002",
                "PROFESSIONAL", LocalDate.now().plusYears(2), "09181234567"
        );
        DriverView result = service.registerDriver(cmd);

        assertNotNull(result);
        assertEquals("EMP-002", result.employeeNumber());
        assertEquals("Maria", result.firstName());
        assertEquals("ACTIVE", result.employmentStatus());
        assertFalse(result.licenseExpired());
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void registerDriver_duplicateEmployeeNumber_throwsConflict() {
        when(driverRepository.findByEmployeeNumber("EMP-001")).thenReturn(Optional.of(existingDriver));

        CreateDriverCommand cmd = new CreateDriverCommand(
                "emp-001", "Pedro", "Santos", "LIC-999",
                "PROFESSIONAL", LocalDate.now().plusYears(2), null
        );
        assertThrows(BusinessConflictException.class, () -> service.registerDriver(cmd));
    }

    @Test
    void registerDriver_duplicateLicenseNumber_throwsConflict() {
        when(driverRepository.findByEmployeeNumber("EMP-099")).thenReturn(Optional.empty());
        when(driverRepository.findByLicenseNumber("LIC-001")).thenReturn(Optional.of(existingDriver));

        CreateDriverCommand cmd = new CreateDriverCommand(
                "emp-099", "Pedro", "Santos", "LIC-001",
                "PROFESSIONAL", LocalDate.now().plusYears(2), null
        );
        assertThrows(BusinessConflictException.class, () -> service.registerDriver(cmd));
    }

    @Test
    void registerDriver_invalidLicenseType_throwsValidation() {
        when(driverRepository.findByEmployeeNumber("EMP-099")).thenReturn(Optional.empty());
        when(driverRepository.findByLicenseNumber("LIC-099")).thenReturn(Optional.empty());

        CreateDriverCommand cmd = new CreateDriverCommand(
                "emp-099", "Pedro", "Santos", "LIC-099",
                "INVALID_TYPE", LocalDate.now().plusYears(2), null
        );
        assertThrows(CommandValidationException.class, () -> service.registerDriver(cmd));
    }

    @Test
    void changeEmploymentStatus_success() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existingDriver));

        DriverView result = service.changeEmploymentStatus(1L, "SUSPENDED");
        assertEquals("SUSPENDED", result.employmentStatus());
    }

    @Test
    void changeEmploymentStatus_notFound_throwsNotFound() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.changeEmploymentStatus(99L, "ACTIVE"));
    }

    @Test
    void archiveDriver_success() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existingDriver));
        service.archiveDriver(1L);
        assertNotNull(existingDriver.getArchivedAt());
    }

    @Test
    void searchDrivers_returnsPagedResults() {
        Page<Driver> page = new PageImpl<>(List.of(existingDriver));
        when(driverRepository.findByArchivedAtIsNull(any())).thenReturn(page);

        Page<DriverSummaryView> result = service.searchDrivers(null, PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals("EMP-001", result.getContent().get(0).employeeNumber());
    }
}
