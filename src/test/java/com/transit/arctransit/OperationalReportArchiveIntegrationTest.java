package com.transit.arctransit;

import com.transit.arctransit.common.exception.BusinessConflictException;
import com.transit.arctransit.common.exception.CommandValidationException;
import com.transit.arctransit.common.exception.ResourceNotFoundException;
import com.transit.arctransit.fleet.*;
import com.transit.arctransit.incident.*;
import com.transit.arctransit.reporting.*;
import com.transit.arctransit.reporting.domain.OperationalReportRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "ARC_TEST_POSTGRES", matches = "true")
class OperationalReportArchiveIntegrationTest {
    @Autowired OperationalReportService reports;
    @Autowired FleetManagementService fleet;
    @Autowired IncidentManagementService incidents;
    @Autowired JdbcTemplate jdbc;
    @Autowired OperationalReportRepository reportRepository;
    @Autowired TransactionTemplate transactions;
    @Value("${arc.security.admin.username}") String administrator;

    @BeforeEach
    void authenticate() { as("SYSTEM_ADMIN"); }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void archiveAndRestoreSeparateActiveListsCountsAndReviewWithoutChangingVerification() {
        var report = report();
        long unverified = reports.countUnverifiedReports();
        long stale = reports.countStaleReports();
        reports.archiveReport(report.id());
        reports.archiveReport(report.id());
        assertNotNull(reports.getReport(report.id()).archivedAt());
        assertEquals("UNVERIFIED", reports.getReport(report.id()).verificationStatus());
        assertEquals(unverified - 1, reports.countUnverifiedReports());
        assertEquals(stale - 1, reports.countStaleReports());
        assertFalse(reports.searchReports(PageRequest.of(0, 1000)).stream().anyMatch(row -> row.id().equals(report.id())));
        assertFalse(reports.getReviewCandidates(Instant.now()).stream().anyMatch(row -> row.id().equals(report.id())));
        var archived = reports.searchArchivedReports(report.description().toLowerCase(), PageRequest.of(0, 1));
        assertEquals(1, archived.getTotalElements());
        assertEquals(report.id(), archived.getContent().getFirst().id());
        assertTrue(reports.searchArchivedReports("  ", PageRequest.of(0, 1000)).stream().anyMatch(row -> row.id().equals(report.id())));
        assertThrows(CommandValidationException.class, () -> reports.verifyReport(report.id(), "CONFIRMED", "Inspection evidence."));
        assertEquals(1, reports.getVerificationHistory(report.id()).size());
        assertEquals(1, audit("OPERATIONAL_REPORT_ARCHIVED", report.id()));
        reports.restoreReport(report.id());
        reports.restoreReport(report.id());
        assertNull(reports.getReport(report.id()).archivedAt());
        assertEquals(unverified, reports.countUnverifiedReports());
        assertEquals(stale, reports.countStaleReports());
        assertEquals(0, reports.searchArchivedReports(report.description(), PageRequest.of(0, 10)).getTotalElements());
        assertTrue(reports.searchReports(PageRequest.of(0, 1000)).stream().anyMatch(row -> row.id().equals(report.id())));
        assertEquals(1, audit("OPERATIONAL_REPORT_RESTORED", report.id()));
        reports.verifyReport(report.id(), "CONFIRMED", "Workshop inspection evidence.");
        reports.archiveReport(report.id());
        reports.restoreReport(report.id());
        assertEquals("CONFIRMED", reports.getReport(report.id()).verificationStatus());
        assertEquals(2, reports.getVerificationHistory(report.id()).size());
    }

    @Test
    void archivedReportDeletionRetainsImmutableHistoryAndSnapshotAudit() {
        var report = report();
        assertThrows(BusinessConflictException.class, () -> reports.hardDeleteReport(report.id()));
        reports.verifyReport(report.id(), "REJECTED", "Duplicate radio report.");
        reports.archiveReport(report.id());
        long history = count("SELECT count(*) FROM arc.report_verification_events WHERE report_id=?", report.id());
        reports.hardDeleteReport(report.id());
        assertThrows(ResourceNotFoundException.class, () -> reports.getReport(report.id()));
        assertEquals(0, reports.searchArchivedReports(report.description(), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(history, count("SELECT count(*) FROM arc.report_verification_events WHERE report_id=?", report.id()));
        String snapshot = jdbc.queryForObject("SELECT details FROM arc.audit_logs WHERE action_name='OPERATIONAL_REPORT_DELETED' AND entity_id=?",
                String.class, report.id());
        assertTrue(snapshot.contains(report.description()));
        assertTrue(snapshot.contains("Duplicate radio report."));
        assertTrue(snapshot.contains("archivedAt="));
        assertEquals(1, audit("OPERATIONAL_REPORT_SUBMITTED", report.id()));
        assertEquals(1, audit("OPERATIONAL_REPORT_ARCHIVED", report.id()));
        assertThrows(RuntimeException.class, () -> jdbc.update("UPDATE arc.report_verification_events SET reason='Changed' WHERE report_id=?", report.id()));
        assertThrows(RuntimeException.class, () -> jdbc.update("DELETE FROM arc.report_verification_events WHERE report_id=?", report.id()));
        assertThrows(RuntimeException.class, () -> jdbc.update("""
                INSERT INTO arc.report_verification_events(report_id,new_status,recorded_by,recorded_at,reason)
                VALUES (?,'UNVERIFIED','probe',now(),'Invalid orphan insert')
                """, report.id()));
        assertThrows(RuntimeException.class, () -> insertException(report.id()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"incident", "exception"})
    void realOperationalLinksBlockDeletionAndRemainIntact(String link) {
        var report = report();
        if (link.equals("incident")) {
            incidents.reportIncident(new CreateIncidentCommand("Linked radio concern", "Retained linked incident", "LOW",
                    "FLEET", report.entityId(), Instant.now().minusSeconds(1), report.id()));
        } else insertException(report.id());
        reports.archiveReport(report.id());
        String message = assertThrows(BusinessConflictException.class, () -> reports.hardDeleteReport(report.id())).getMessage();
        assertTrue(message.contains(link.equals("incident") ? "incidents" : "reconciliation exceptions"));
        assertNotNull(reports.getReport(report.id()).archivedAt());
        assertEquals(0, audit("OPERATIONAL_REPORT_DELETED", report.id()));
    }

    @Test
    void staffCannotArchiveRestoreDeleteOrBrowseTheReportArchive() {
        var report = report();
        as("OPERATIONS_STAFF");
        assertThrows(AccessDeniedException.class, () -> reports.archiveReport(report.id()));
        assertThrows(AccessDeniedException.class, () -> reports.restoreReport(report.id()));
        assertThrows(AccessDeniedException.class, () -> reports.hardDeleteReport(report.id()));
        assertThrows(AccessDeniedException.class, () -> reports.searchArchivedReports("", PageRequest.of(0, 10)));
        assertNull(reports.getReport(report.id()).archivedAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARCHIVED", "RESTORED", "DELETED"})
    void auditFailureRollsBackArchiveRestoreOrDeletion(String action) {
        var report = report();
        if (!action.equals("ARCHIVED")) reports.archiveReport(report.id());
        jdbc.execute("""
                CREATE FUNCTION arc.reject_report_archive_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.action_name = 'OPERATIONAL_REPORT_' || TG_ARGV[0] THEN
                        RAISE EXCEPTION 'Test report audit unavailable' USING ERRCODE='23514';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER reject_report_archive_audit BEFORE INSERT ON arc.audit_logs "
                + "FOR EACH ROW EXECUTE FUNCTION arc.reject_report_archive_audit('" + action + "')");
        try {
            assertThrows(RuntimeException.class, () -> {
                switch (action) {
                    case "ARCHIVED" -> reports.archiveReport(report.id());
                    case "RESTORED" -> reports.restoreReport(report.id());
                    default -> reports.hardDeleteReport(report.id());
                }
            });
            assertEquals(action.equals("ARCHIVED"), reports.getReport(report.id()).archivedAt() == null);
            assertEquals(1, reports.getVerificationHistory(report.id()).size());
            assertEquals(0, audit("OPERATIONAL_REPORT_" + action, report.id()));
        } finally {
            jdbc.execute("DROP TRIGGER reject_report_archive_audit ON arc.audit_logs");
            jdbc.execute("DROP FUNCTION arc.reject_report_archive_audit()");
        }
    }

    @Test
    void concurrentExceptionReferenceWaitsForDeletionAndCannotBecomeAnOrphan() throws Exception {
        var report = report();
        reports.archiveReport(report.id());
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch inserting = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var deletion = executor.submit(() -> {
                as("SYSTEM_ADMIN");
                try {
                    transactions.executeWithoutResult(status -> {
                        reportRepository.findLockedById(report.id()).orElseThrow();
                        locked.countDown();
                        assertDoesNotThrow(() -> assertTrue(release.await(10, TimeUnit.SECONDS)));
                        reports.hardDeleteReport(report.id());
                    });
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            assertTrue(locked.await(10, TimeUnit.SECONDS));
            var reference = executor.submit(() -> {
                inserting.countDown();
                return assertThrows(RuntimeException.class, () -> insertException(report.id()));
            });
            assertTrue(inserting.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> reference.get(300, TimeUnit.MILLISECONDS));
            release.countDown();
            deletion.get(10, TimeUnit.SECONDS);
            assertNotNull(reference.get(10, TimeUnit.SECONDS));
            assertEquals(0, count("SELECT count(*) FROM arc.operational_reports WHERE id=?", report.id()));
            assertEquals(0, count("SELECT count(*) FROM arc.operational_exceptions WHERE affected_entity_type='OPERATIONAL_REPORT' AND affected_entity_id=?", report.id()));
            assertEquals(1, audit("OPERATIONAL_REPORT_DELETED", report.id()));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private OperationalReportView report() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var bus = fleet.createUnit(new CreateFleetUnitCommand("ARC-" + suffix, "QA" + suffix, (short) 40));
        return reports.submitReport(new CreateOperationalReportCommand("RADIO", "FLEET", bus.id(),
                Instant.now().minusSeconds(24 * 3600), "Archive radio sample " + suffix));
    }

    private void insertException(Long id) {
        jdbc.update("""
                INSERT INTO arc.operational_exceptions(exception_type,severity,affected_entity_type,affected_entity_id,
                    description,exception_status,detected_at,rule_code)
                VALUES ('STALE_STATE','LOW','OPERATIONAL_REPORT',?,'Retained review case','RESOLVED',now(),'STALE_OPERATIONAL_REPORT')
                """, id);
    }

    private long count(String sql, Long id) { return jdbc.queryForObject(sql, Long.class, id); }
    private long audit(String action, Long id) {
        return jdbc.queryForObject("SELECT count(*) FROM arc.audit_logs WHERE action_name=? AND entity_id=?", Long.class, action, id);
    }
    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(administrator, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
