package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrowthCalendarRepository extends JpaRepository<GrowthCalendar, Long> {

    /**
     * Returns the growth calendar entries matching the given filters.
     *
     * <p>Both parameters are optional: a {@code null} parameter disables its own
     * filter. A filter that matches nothing yields an empty list, never an error.
     *
     * @param farmId    farm identifier, or {@code null} to ignore the farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} to ignore the block
     * @return the matching entries, ordered by block then planting date
     */
    @Query("""
            SELECT c FROM GrowthCalendar c
            WHERE (:farmId IS NULL OR c.farmId = :farmId)
              AND (:blockCode IS NULL OR c.blockCode = :blockCode)
            ORDER BY c.blockCode ASC, c.plantingDate ASC NULLS LAST, c.id ASC
            """)
    List<GrowthCalendar> findByOptionalFilters(@Param("farmId") Integer farmId,
                                                 @Param("blockCode") String blockCode);
}
