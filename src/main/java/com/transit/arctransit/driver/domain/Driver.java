package com.transit.arctransit.driver.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Driver identity and employment record in the Arc Transit fleet.
 *
 * Maps to the arc.drivers table created by V3__create_drivers.sql.
 *
 * Drivers are operational staff records. They do NOT automatically
 * receive login accounts (as stated in V2 authentication migration).
 *
 * Design decisions follow the same patterns as FleetUnit.java and AppUser.java:
 * - @Enumerated(EnumType.STRING) for type-safe enum mapping
 * - @Version for optimistic locking
 * - Soft-delete via archivedAt
 *
 * License expiry check:
 *   The isLicenseExpired() method compares licenseExpiryDate against
 *   LocalDate.now(). The dispatch service uses this check to reject
 *   assignments to drivers with expired licenses.
 */
@Entity
@Table(name = "drivers", schema = "arc")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_number", length = 30, nullable = false, unique = true)
    private String employeeNumber;

    @Column(name = "first_name", length = 80, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 80, nullable = false)
    private String lastName;

    @Column(name = "license_number", length = 50, nullable = false, unique = true)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_type", length = 30, nullable = false)
    private LicenseType licenseType;

    @Column(name = "license_expiry_date", nullable = false)
    private LocalDate licenseExpiryDate;

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", length = 30, nullable = false)
    private EmploymentStatus employmentStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private Long version;

    /** JPA requires a protected no-args constructor. */
    protected Driver() {
    }

    /**
     * Creates a new driver with ACTIVE employment status.
     */
    public Driver(String employeeNumber, String firstName, String lastName,
                  String licenseNumber, LicenseType licenseType,
                  LocalDate licenseExpiryDate, String contactNumber) {
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.licenseNumber = licenseNumber;
        this.licenseType = licenseType;
        this.licenseExpiryDate = licenseExpiryDate;
        this.contactNumber = contactNumber;
        this.employmentStatus = EmploymentStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // --- Domain Methods ---

    /** Returns true if the driver's license has expired. */
    public boolean isLicenseExpired() {
        return LocalDate.now().isAfter(licenseExpiryDate);
    }

    /** Returns the driver's full name. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** Changes the employment status. */
    public void changeEmploymentStatus(EmploymentStatus newStatus) {
        this.employmentStatus = newStatus;
        this.updatedAt = Instant.now();
    }

    /** Soft-deletes this driver by setting the archive timestamp. */
    public void archive() {
        this.archivedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Restores this driver from the archive. */
    public void unarchive() {
        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    /** Returns true when this driver has not been archived. */
    public boolean isNotArchived() {
        return archivedAt == null;
    }

    /** Updates driver details. */
    public void updateDetails(String employeeNumber, String firstName, String lastName,
                              String licenseNumber, LicenseType licenseType,
                              LocalDate licenseExpiryDate, String contactNumber) {
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.licenseNumber = licenseNumber;
        this.licenseType = licenseType;
        this.licenseExpiryDate = licenseExpiryDate;
        this.contactNumber = contactNumber;
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getLicenseNumber() { return licenseNumber; }
    public LicenseType getLicenseType() { return licenseType; }
    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
    public String getContactNumber() { return contactNumber; }
    public EmploymentStatus getEmploymentStatus() { return employmentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getArchivedAt() { return archivedAt; }
}
