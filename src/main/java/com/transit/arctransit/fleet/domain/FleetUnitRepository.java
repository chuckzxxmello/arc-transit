package com.transit.arctransit.fleet.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// data access for fleet-unit master record list
public interface FleetUnitRepository extends JpaRepository<FleetUnit, Long> {

    // lookup for existing bus fleet data
    Optional<FleetUnit> findByUnitNumber(
    									String unitNumber
    );

    Optional<FleetUnit> findByPlateNumber(
    									String plateNumber
    );

    // checks if it exists
    boolean existsByUnitNumber(
            				String unitNumber
    );

    boolean existsByPlateNumber(
            				String plateNumber
    );

    boolean existsByUnitNumberAndIdNot(
						            String unitNumber,
						            Long id
    );

    boolean existsByPlateNumberAndIdNot(
						            String plateNumber,
						            Long id
    );

    // archive queries
    
    // return the only non-archived fleet units
    Page<FleetUnit> findByArchivedAtIsNull(
            							Pageable pageable
    );

    // Returns only archived fleet units.
    Page<FleetUnit> findByArchivedAtIsNotNull(
            								Pageable pageable
    );

    // search for active bus fleet units
    @org.springframework.data.jpa.repository.Query(
    						   """
					            SELECT unit
					            FROM FleetUnit unit
					            WHERE unit.archivedAt IS NULL
					              AND (
					                    :searchFilter IS NULL
					                    OR :searchFilter = ''
					                    OR LOWER(unit.unitNumber)
					                       LIKE LOWER(CONCAT('%', :searchFilter, '%'))
					                    OR LOWER(unit.plateNumber)
					                       LIKE LOWER(CONCAT('%', :searchFilter, '%'))
					              )
					              AND (
					                    :status IS NULL
					                    OR unit.operationalStatus = :status
					              )
					            """)
    
    Page<FleetUnit> searchActive(
				            @Param("searchFilter") String searchFilter,
				            @Param("status") OperationalStatus status,

				            Pageable pageable
    );
}