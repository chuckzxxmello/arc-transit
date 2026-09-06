package com.transit.arctransit.driver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;

// driver identity and employment records

@Entity
@Table(name = "drivers", schema = "arc")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "employee_number",
            length = 30,
            nullable = false,
            unique = true
    )
    private String employeeNumber;

    @Column(
            name = "first_name",
            length = 80,
            nullable = false
    )
    private String firstName;

    @Column(
            name = "last_name",
            length = 80,
            nullable = false
    )
    private String lastName;

    @Column(
            name = "license_number",
            length = 50,
            nullable = false,
            unique = true
    )
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "license_type",
            length = 30,
            nullable = false
    )
    private LicenseType licenseType;

    @Column(
            name = "license_expiry_date",
            nullable = false
    )
    private LocalDate licenseExpiryDate;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "employment_status",
            length = 30,
            nullable = false
    )
    private EmploymentStatus employmentStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    // JPA

    protected Driver() {
    }

    // create driver

    public Driver(
            String employeeNumber,
            String firstName,
            String lastName,
            String licenseNumber,
            LicenseType licenseType,
            LocalDate licenseExpiryDate,
            String contactNumber) {

        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.licenseNumber = licenseNumber;
        this.licenseType = licenseType;
        this.licenseExpiryDate = licenseExpiryDate;
        this.contactNumber = contactNumber;
        this.employmentStatus = EmploymentStatus.ACTIVE;

        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    // license domain logic

    public boolean isLicenseExpired() {
        return LocalDate.now().isAfter(licenseExpiryDate);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // employment status

    public void changeEmploymentStatus(
            EmploymentStatus newStatus) {

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Employment status cannot be null."
            );
        }

        this.employmentStatus = newStatus;
        this.updatedAt = Instant.now();
    }

    // archive

    public void archive() {

        if (archivedAt != null) {
            return;
        }

        Instant now = Instant.now();

        this.archivedAt = now;
        this.updatedAt = now;
    }

    // restore

    public void unarchive() {

        if (archivedAt == null) {
            return;
        }

        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    // archive state

    public boolean isArchived() {
        return archivedAt != null;
    }

    public boolean isNotArchived() {
        return !isArchived();
    }

    // update details

    public void updateDetails(
            String employeeNumber,
            String firstName,
            String lastName,
            String licenseNumber,
            LicenseType licenseType,
            LocalDate licenseExpiryDate,
            String contactNumber) {

        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.licenseNumber = licenseNumber;
        this.licenseType = licenseType;
        this.licenseExpiryDate = licenseExpiryDate;
        this.contactNumber = contactNumber;
        this.updatedAt = Instant.now();
    }

    // getters

    public Long getId() {
        return id;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public LocalDate getLicenseExpiryDate() {
        return licenseExpiryDate;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}