package com.transit.arctransit.reconciliation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationalExceptionRepository extends JpaRepository<OperationalException, Long> {

    Page<OperationalException> findAllByOrderByDetectedAtDesc(Pageable pageable);

    List<OperationalException> findByExceptionStatusIn(List<ExceptionStatus> statuses);

    long countByExceptionStatusIn(List<ExceptionStatus> statuses);
    
    long countByExceptionTypeAndExceptionStatusIn(ExceptionType type, List<ExceptionStatus> statuses);
    
    long countBySeverityAndExceptionStatusIn(String severity, List<ExceptionStatus> statuses);
    
    boolean existsByExceptionTypeAndAffectedEntityTypeAndAffectedEntityIdAndExceptionStatusIn(
            ExceptionType type, String entityType, Long entityId, List<ExceptionStatus> statuses);
}
