package com.transit.arctransit.reporting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OperationalReportRepository extends JpaRepository<OperationalReport, Long> {

    @Query("""
            select r from OperationalReport r
            where r.archivedAt is null and (r.verificationStatus in (UNVERIFIED, DISPUTED)
               or (r.effectiveAt <= :staleBefore and r.verificationStatus <> REJECTED))
            """)
    List<OperationalReport> findReviewCandidates(Instant staleBefore);

    Page<OperationalReport> findByArchivedAtIsNull(Pageable pageable);

    // PostgreSQL key-share reference inserts must wait until deletion checks commit.
    @Query(value = "SELECT * FROM arc.operational_reports WHERE id = :id FOR UPDATE", nativeQuery = true)
    java.util.Optional<OperationalReport> findLockedById(@Param("id") Long id);

    @Query("""
            select r from OperationalReport r where r.archivedAt is not null
            and lower(concat(r.id, ' ', r.entityType, ' ', r.entityId, ' ', r.description, ' ',
                r.reportedBy, ' ', r.sourceType, ' ', r.verificationStatus)) like lower(concat('%', :filter, '%'))
            """)
    Page<OperationalReport> searchArchived(@Param("filter") String filter, Pageable pageable);

    @Query(value = """
            SELECT 'incidents' WHERE EXISTS (
                SELECT 1 FROM arc.incidents WHERE operational_report_id = :id)
            UNION ALL SELECT 'reconciliation exceptions' WHERE EXISTS (
                SELECT 1 FROM arc.operational_exceptions
                WHERE affected_entity_type = 'OPERATIONAL_REPORT' AND affected_entity_id = :id)
            """, nativeQuery = true)
    List<String> findPermanentDeletionBlockers(@Param("id") Long id);

    long countByArchivedAtIsNullAndVerificationStatus(VerificationStatus status);

    long countByArchivedAtIsNullAndEffectiveAtLessThanEqualAndVerificationStatusNot(
            Instant staleBefore, VerificationStatus status);
}
