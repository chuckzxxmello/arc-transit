package com.transit.arctransit.driver.application;

// driver module

import com.transit.arctransit.driver.CreateDriverCommand;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverQuery;
import com.transit.arctransit.driver.DriverSummaryView;
import com.transit.arctransit.driver.DriverView;
import com.transit.arctransit.driver.UpdateDriverCommand;

// driver domain

import com.transit.arctransit.driver.domain.Driver;
import com.transit.arctransit.driver.domain.DriverRepository;
import com.transit.arctransit.driver.domain.EmploymentStatus;
import com.transit.arctransit.driver.domain.LicenseType;

// common and audit

import com.transit.arctransit.audit.AuditRecordingService;
import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;

// java

import java.util.Locale;

// spring

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// driver management service

@Service
@Transactional
public class AppDriverManagementService
        implements DriverManagementService {

    // constants

    private static final Logger log =
            LoggerFactory.getLogger(
                    AppDriverManagementService.class
            );

    private static final String SYSTEM_ADMIN_ROLE =
            "SYSTEM_ADMIN";

    private static final String OPERATIONS_STAFF_ROLE =
            "OPERATIONS_STAFF";

    // dependencies

    private final DriverRepository driverRepository;
    private final AuditRecordingService auditService;

    public AppDriverManagementService(
            DriverRepository driverRepository,
            AuditRecordingService auditService) {

        this.driverRepository = driverRepository;
        this.auditService = auditService;
    }

    // register driver

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DriverView registerDriver(
            CreateDriverCommand command) {

        String normalizedEmployeeNumber =
                normalizeEmployeeNumber(
                        command.employeeNumber()
                );

        String normalizedLicenseNumber =
                normalizeLicenseNumber(
                        command.licenseNumber()
                );

        // check duplicate employee number

        if (driverRepository
                .findByEmployeeNumber(
                        normalizedEmployeeNumber
                )
                .isPresent()) {

            throw new BusinessConflictException(
                    "Employee number is already in use: "
                            + normalizedEmployeeNumber
            );
        }

        // check duplicate license number

        if (driverRepository
                .findByLicenseNumber(
                        normalizedLicenseNumber
                )
                .isPresent()) {

            throw new BusinessConflictException(
                    "License number is already in use: "
                            + normalizedLicenseNumber
            );
        }

        LicenseType licenseType =
                parseLicenseType(
                        command.licenseType()
                );

        Driver driver = new Driver(
                normalizedEmployeeNumber,
                command.firstName().trim(),
                command.lastName().trim(),
                normalizedLicenseNumber,
                licenseType,
                command.licenseExpiryDate(),
                normalizeOptionalContact(
                        command.contactNumber()
                )
        );

        Driver saved =
                driverRepository.save(driver);

        auditService.recordAction(
                "DRIVER_REGISTERED",
                "Driver",
                saved.getId(),
                "Registered driver "
                        + saved.getFullName()
        );

        log.info(
                "Registered driver '{}'.",
                saved.getEmployeeNumber()
        );

        return toView(saved);
    }

    // update driver

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DriverView updateDriver(
            UpdateDriverCommand command) {

        Driver driver =
                driverRepository.findById(command.id())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver not found: "
                                                + command.id()
                                )
                        );

        String normalizedEmployeeNumber =
                normalizeEmployeeNumber(
                        command.employeeNumber()
                );

        String normalizedLicenseNumber =
                normalizeLicenseNumber(
                        command.licenseNumber()
                );

        // check duplicate employee number

        driverRepository
                .findByEmployeeNumber(
                        normalizedEmployeeNumber
                )
                .filter(existing ->
                        !existing.getId().equals(
                                driver.getId()
                        )
                )
                .ifPresent(existing -> {
                    throw new BusinessConflictException(
                            "Employee number is already in use: "
                                    + normalizedEmployeeNumber
                    );
                });

        // check duplicate license number

        driverRepository
                .findByLicenseNumber(
                        normalizedLicenseNumber
                )
                .filter(existing ->
                        !existing.getId().equals(
                                driver.getId()
                        )
                )
                .ifPresent(existing -> {
                    throw new BusinessConflictException(
                            "License number is already in use: "
                                    + normalizedLicenseNumber
                    );
                });

        LicenseType licenseType =
                parseLicenseType(
                        command.licenseType()
                );

        driver.updateDetails(
                normalizedEmployeeNumber,
                command.firstName().trim(),
                command.lastName().trim(),
                normalizedLicenseNumber,
                licenseType,
                command.licenseExpiryDate(),
                normalizeOptionalContact(
                        command.contactNumber()
                )
        );

        auditService.recordAction(
                "DRIVER_UPDATED",
                "Driver",
                driver.getId(),
                "Updated details for driver "
                        + driver.getFullName()
        );

        log.info(
                "Updated driver '{}'.",
                driver.getEmployeeNumber()
        );

        return toView(driver);
    }

    // change employment status

    @Override
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DriverView changeEmploymentStatus(
            Long id,
            String newStatus) {

        Driver driver =
                driverRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver not found: "
                                                + id
                                )
                        );

        EmploymentStatus status =
                parseEmploymentStatus(newStatus);

        driver.changeEmploymentStatus(status);

        auditService.recordAction(
                "DRIVER_STATUS_CHANGED",
                "Driver",
                driver.getId(),
                "Status changed to "
                        + status.name()
                        + " for driver "
                        + driver.getFullName()
        );

        log.info(
                "Changed driver '{}' status to '{}'.",
                driver.getEmployeeNumber(),
                status
        );

        return toView(driver);
    }

    // archive driver

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveDriver(Long id) {

        Driver driver =
                driverRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver not found: "
                                                + id
                                )
                        );

        driver.archive();

        auditService.recordAction(
                "DRIVER_ARCHIVED",
                "Driver",
                id,
                "Archived driver "
                        + driver.getFullName()
        );

        log.info(
                "Archived driver '{}'.",
                driver.getEmployeeNumber()
        );
    }

    // unarchive driver

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveDriver(Long id) {

        Driver driver =
                driverRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver not found: "
                                                + id
                                )
                        );

        driver.unarchive();

        auditService.recordAction(
                "DRIVER_UNARCHIVED",
                "Driver",
                id,
                "Unarchived driver "
                        + driver.getFullName()
        );

        log.info(
                "Unarchived driver '{}'.",
                driver.getEmployeeNumber()
        );
    }

    // permanently delete driver

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteDriver(Long id) {

        Driver driver =
                driverRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver not found: "
                                                + id
                                )
                        );

        try {

            // flush immediately so database constraint errors
            // are raised inside this try/catch

            driverRepository.delete(driver);
            driverRepository.flush();

        } catch (DataIntegrityViolationException exception) {

            throw new BusinessConflictException(
                    "Cannot permanently delete driver "
                            + id
                            + " because the driver is "
                            + "referenced by other records."
            );
        }

        log.info(
                "Permanently deleted driver '{}'.",
                driver.getEmployeeNumber()
        );
    }

    // search active drivers

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public Page<DriverSummaryView> searchDrivers(
            DriverQuery query,
            Pageable pageable) {

        // query filtering will be moved to the repository
        // when the DriverRepository search method is added

        return driverRepository
                .findByArchivedAtIsNull(pageable)
                .map(this::toSummaryView);
    }

    // search archived drivers

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public Page<DriverSummaryView> searchArchivedDrivers(
            Pageable pageable) {

        return driverRepository
                .findByArchivedAtIsNotNull(pageable)
                .map(this::toSummaryView);
    }

    // get driver

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(
            "hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')"
    )
    public DriverView getDriver(Long id) {

        Driver driver =
                driverRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Driver not found: "
                                                + id
                                )
                        );

        return toView(driver);
    }

    // map driver to full view

    private DriverView toView(
            Driver driver) {

        return new DriverView(
                driver.getId(),
                driver.getEmployeeNumber(),
                driver.getFirstName(),
                driver.getLastName(),
                driver.getLicenseNumber(),
                driver.getLicenseType().name(),
                driver.getLicenseExpiryDate().toString(),
                driver.getContactNumber(),
                driver.getEmploymentStatus().name(),
                driver.isLicenseExpired()
        );
    }

    // map driver to summary view

    private DriverSummaryView toSummaryView(
            Driver driver) {

        return new DriverSummaryView(
                driver.getId(),
                driver.getEmployeeNumber(),
                driver.getFullName(),
                driver.getLicenseNumber(),
                driver.getLicenseExpiryDate().toString(),
                driver.getEmploymentStatus().name(),
                driver.isLicenseExpired()
        );
    }

    // normalize employee number

    private static String normalizeEmployeeNumber(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "Employee number cannot be blank."
            );
        }

        return value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    // normalize license number

    private static String normalizeLicenseNumber(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "License number cannot be blank."
            );
        }

        return value.trim();
    }

    // normalize optional contact number

    private static String normalizeOptionalContact(
            String value) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    // parse license type

    private static LicenseType parseLicenseType(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "License type cannot be blank."
            );
        }

        try {

            return LicenseType.valueOf(
                    value
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException(
                    "Invalid license type: "
                            + value
            );
        }
    }

    // parse employment status

    private static EmploymentStatus parseEmploymentStatus(
            String value) {

        if (value == null || value.isBlank()) {

            throw new CommandValidationException(
                    "Employment status cannot be blank."
            );
        }

        try {

            return EmploymentStatus.valueOf(
                    value
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

        } catch (IllegalArgumentException exception) {

            throw new CommandValidationException(
                    "Invalid employment status: "
                            + value
            );
        }
    }
}