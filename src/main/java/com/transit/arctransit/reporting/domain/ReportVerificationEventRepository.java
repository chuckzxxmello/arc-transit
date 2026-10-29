package com.transit.arctransit.reporting.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportVerificationEventRepository extends JpaRepository<ReportVerificationEvent, Long> {

    List<ReportVerificationEvent> findByReportIdOrderByRecordedAtAscIdAsc(Long reportId);
}
