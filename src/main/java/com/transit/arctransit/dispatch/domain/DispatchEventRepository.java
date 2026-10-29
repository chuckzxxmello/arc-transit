package com.transit.arctransit.dispatch.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchEventRepository extends JpaRepository<DispatchEvent, Long> {

    List<DispatchEvent> findByAssignmentIdOrderByRecordedAtAscIdAsc(Long assignmentId);
}
