package com.transit.arctransit;

import com.transit.arctransit.driver.CreateDriverCommand;
import com.transit.arctransit.driver.DriverManagementService;
import com.transit.arctransit.driver.DriverQuery;
import com.transit.arctransit.driver.DriverSummaryView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class DriverSearchIntegrationTest {

    @Autowired
    private DriverManagementService driverService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "driver-search-test", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blankAndMissingSearchLoadTheDriverGrid() {
        Long id = createDriver("GRID-" + suffix(), "Casey", "Transit");
        DriverQuery[] queries = {null, new DriverQuery(null, null),
                new DriverQuery("", null), new DriverQuery("   ", null)};

        for (DriverQuery query : queries) {
            var page = driverService.searchDrivers(query, PageRequest.of(0, 50));
            assertTrue(page.getContent().stream().anyMatch(driver -> id.equals(driver.id())));
        }
    }

    @Test
    void searchMatchesEmployeeNumberNameAndLicenseWithoutCaseSensitivity() {
        String suffix = suffix();
        String employeeNumber = "GRID-" + suffix;
        Long id = createDriver(employeeNumber, "Casey", "Transit" + suffix);
        for (String search : List.of(employeeNumber.toLowerCase(), "casey transit" + suffix.toLowerCase(),
                "lic-" + employeeNumber.toLowerCase())) {
            var page = driverService.searchDrivers(new DriverQuery(search, null), PageRequest.of(0, 10));
            assertEquals(List.of(id), page.getContent().stream().map(DriverSummaryView::id).toList());
        }
    }

    @Test
    void statusArchiveSortingAndPaginationRemainCorrect() {
        String prefix = "GRID-" + suffix();
        Long first = createDriver(prefix + "-A", "First", "Driver");
        Long second = createDriver(prefix + "-B", "Second", "Driver");
        Long archived = createDriver(prefix + "-C", "Archived", "Driver");
        driverService.changeEmploymentStatus(second, "INACTIVE");
        driverService.archiveDriver(archived);

        var active = driverService.searchDrivers(new DriverQuery(prefix, "ACTIVE"), PageRequest.of(0, 10));
        assertEquals(List.of(first), active.getContent().stream().map(DriverSummaryView::id).toList());
        var inactive = driverService.searchDrivers(new DriverQuery(prefix, "INACTIVE"), PageRequest.of(0, 10));
        assertEquals(List.of(second), inactive.getContent().stream().map(DriverSummaryView::id).toList());

        var firstPage = driverService.searchDrivers(new DriverQuery(prefix, null),
                PageRequest.of(0, 1, Sort.by("employeeNumber")));
        var secondPage = driverService.searchDrivers(new DriverQuery(prefix, null),
                PageRequest.of(1, 1, Sort.by("employeeNumber")));
        assertEquals(2, firstPage.getTotalElements());
        assertEquals(first, firstPage.getContent().getFirst().id());
        assertEquals(second, secondPage.getContent().getFirst().id());

        var statusOnly = driverService.searchDrivers(new DriverQuery(null, "INACTIVE"), PageRequest.of(0, 50));
        assertTrue(statusOnly.getContent().stream().anyMatch(driver -> second.equals(driver.id())));
    }

    @Test
    void dispatchChoicesSearchBeyondOneHundredAndExcludeIneligibleBeforePaging() {
        String prefix = "PICK-" + suffix();
        for (int index = 0; index < 105; index++) {
            createDriver(prefix + "-" + String.format("%03d", index), "Candidate", "Driver");
        }
        Long inactive = createDriver(prefix + "-INACTIVE", "Inactive", "Driver");
        driverService.changeEmploymentStatus(inactive, "INACTIVE");
        Long archived = createDriver(prefix + "-ARCHIVED", "Archived", "Driver");
        driverService.archiveDriver(archived);
        driverService.registerDriver(new CreateDriverCommand(prefix + "-EXPIRED", "Expired", "Driver",
                "LIC-" + prefix + "-EXP", "PROFESSIONAL", LocalDate.now().minusDays(1), null));
        var last = driverService.searchDispatchCandidates(prefix,
                PageRequest.of(2, 50, Sort.by("employeeNumber")));
        assertEquals(105, last.getTotalElements());
        assertEquals(5, last.getNumberOfElements());
        assertTrue(last.getContent().stream().anyMatch(driver -> driver.employeeNumber().endsWith("-104")));
        var search = driverService.searchDispatchCandidates(prefix + "-104", PageRequest.of(0, 50));
        assertEquals(1, search.getTotalElements());
    }

    private Long createDriver(String employeeNumber, String firstName, String lastName) {
        return driverService.registerDriver(new CreateDriverCommand(employeeNumber, firstName, lastName,
                "LIC-" + employeeNumber, "PROFESSIONAL", LocalDate.now().plusYears(2), null)).id();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }
}
