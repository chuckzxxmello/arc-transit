package com.transit.arctransit.reconciliation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface OperationalExceptionRepository extends JpaRepository<OperationalException, Long> {

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(20261015, 16)", nativeQuery = true)
    Integer lockForScan();

    boolean existsByRuleCodeAndAffectedEntityTypeAndAffectedEntityIdAndExceptionStatusIn(
            String ruleCode, String entityType, Long entityId, List<ExceptionStatus> statuses);

    Page<OperationalException> findAllByOrderByDetectedAtDesc(Pageable pageable);

    List<OperationalException> findByExceptionStatusIn(List<ExceptionStatus> statuses);

    long countByExceptionStatusIn(List<ExceptionStatus> statuses);
    
    long countByExceptionTypeAndExceptionStatusIn(ExceptionType type, List<ExceptionStatus> statuses);
    
    long countBySeverityAndExceptionStatusIn(String severity, List<ExceptionStatus> statuses);
    
}
