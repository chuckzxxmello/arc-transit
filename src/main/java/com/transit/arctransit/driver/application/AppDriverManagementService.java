package com.transit.arctransit.driver.application;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.driver.*;
import com.transit.arctransit.driver.domain.Driver;
import com.transit.arctransit.driver.domain.DriverRepository;
import com.transit.arctransit.driver.domain.EmploymentStatus;
import com.transit.arctransit.driver.domain.LicenseType;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal implementation of DriverManagementService.
 *
 * Resides in the driver.application subpackage (Spring Modulith internal).
 *
 * License expiry validation:
 *   When registering a driver, the service warns (but does not block) if
 *   the provided license expiry date is already in the past. The dispatch
 *   service enforces the actual block when creating assignments.
 *
 * Source: https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
 * (Ctrl+F: @PreAuthorize)
 */
@Service
@Transactional
public class AppDriverManagementService implements DriverManagementService {

    private final DriverRepository driverRepository;

    public AppDriverManagementService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DriverView registerDriver(@Valid CreateDriverCommand command) {
        String normalizedEmpNum = command.employeeNumber().trim().toUpperCase();

        if (driverRepository.findByEmployeeNumber(normalizedEmpNum).isPresent()) {
            throw new BusinessConflictException("Employee number is already in use: " + normalizedEmpNum);
        }
        if (driverRepository.findByLicenseNumber(command.licenseNumber().trim()).isPresent()) {
            throw new BusinessConflictException("License number is already in use: " + command.licenseNumber().trim());
        }

        LicenseType licenseType;
        try {
            licenseType = LicenseType.valueOf(command.licenseType());
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException("Invalid license type: " + command.licenseType());
        }

        Driver driver = new Driver(
                normalizedEmpNum,
                command.firstName().trim(),
                command.lastName().trim(),
                command.licenseNumber().trim(),
                licenseType,
                command.licenseExpiryDate(),
                command.contactNumber() != null ? command.contactNumber().trim() : null
        );

        driverRepository.saveAndFlush(driver);
        return toView(driver);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DriverView updateDriver(@Valid UpdateDriverCommand command) {
        Driver driver = driverRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + command.id()));

        String normalizedEmpNum = command.employeeNumber().trim().toUpperCase();

        driverRepository.findByEmployeeNumber(normalizedEmpNum)
                .filter(existing -> !existing.getId().equals(driver.getId()))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("Employee number is already in use: " + normalizedEmpNum);
                });

        driverRepository.findByLicenseNumber(command.licenseNumber().trim())
                .filter(existing -> !existing.getId().equals(driver.getId()))
                .ifPresent(existing -> {
                    throw new BusinessConflictException("License number is already in use: " + command.licenseNumber().trim());
                });

        LicenseType licenseType;
        try {
            licenseType = LicenseType.valueOf(command.licenseType());
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException("Invalid license type: " + command.licenseType());
        }

        driver.updateDetails(
                normalizedEmpNum,
                command.firstName().trim(),
                command.lastName().trim(),
                command.licenseNumber().trim(),
                licenseType,
                command.licenseExpiryDate(),
                command.contactNumber() != null ? command.contactNumber().trim() : null
        );

        return toView(driver);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DriverView changeEmploymentStatus(Long id, String newStatus) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));

        try {
            EmploymentStatus status = EmploymentStatus.valueOf(newStatus);
            driver.changeEmploymentStatus(status);
        } catch (IllegalArgumentException e) {
            throw new CommandValidationException("Invalid employment status: " + newStatus);
        }

        return toView(driver);
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void archiveDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        driver.archive();
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void unarchiveDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        driver.unarchive();
    }

    @Override
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public void hardDeleteDriver(Long id) {
        try {
            driverRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessConflictException("Cannot permanently delete driver " + id + " because they are referenced by other records.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<DriverSummaryView> searchDrivers(DriverQuery query, Pageable pageable) {
        return driverRepository.findByArchivedAtIsNull(pageable)
                .map(driver -> new DriverSummaryView(
                        driver.getId(),
                        driver.getEmployeeNumber(),
                        driver.getFullName(),
                        driver.getLicenseNumber(),
                        driver.getLicenseExpiryDate().toString(),
                        driver.getEmploymentStatus().name(),
                        driver.isLicenseExpired()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public Page<DriverSummaryView> searchArchivedDrivers(Pageable pageable) {
        return driverRepository.findByArchivedAtIsNotNull(pageable)
                .map(driver -> new DriverSummaryView(
                        driver.getId(),
                        driver.getEmployeeNumber(),
                        driver.getFullName(),
                        driver.getLicenseNumber(),
                        driver.getLicenseExpiryDate().toString(),
                        driver.getEmploymentStatus().name(),
                        driver.isLicenseExpired()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('OPERATIONS_STAFF')")
    public DriverView getDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
        return toView(driver);
    }

    private DriverView toView(Driver driver) {
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
}
