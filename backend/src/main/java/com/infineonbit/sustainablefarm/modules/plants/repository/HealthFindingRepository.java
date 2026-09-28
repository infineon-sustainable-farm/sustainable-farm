package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface HealthFindingRepository extends JpaRepository<HealthFinding, Long> {

    /**
     * The findings of the given inspections, with their inspection and their
     * issue. One query serves a whole list of inspections.
     *
     * @param inspectionIds identifiers of the inspections, not empty
     * @return their findings, ordered by inspection then identifier
     */
    @Query("""
            SELECT f FROM HealthFinding f JOIN FETCH f.inspection i JOIN FETCH f.issue
            WHERE i.id IN :inspectionIds
            ORDER BY i.id ASC, f.id ASC
            """)
    List<HealthFinding> findByInspectionIds(@Param("inspectionIds") Collection<Long> inspectionIds);
}
