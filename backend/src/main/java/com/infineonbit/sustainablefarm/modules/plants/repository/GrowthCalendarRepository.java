package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
            WHERE (CAST(:farmId AS Integer) IS NULL OR c.farmId = :farmId)
              AND (CAST(:blockCode AS String) IS NULL OR c.blockCode = :blockCode)
            ORDER BY c.blockCode ASC, c.plantingDate ASC NULLS LAST, c.id ASC
            """)
    List<GrowthCalendar> findByOptionalFilters(@Param("farmId") Integer farmId,
                                                 @Param("blockCode") String blockCode);

    /**
     * Returns the growth calendar rows of one block of one farm, oldest first.
     *
     * <p>Same NULL-safe farm comparison as
     * {@link VarietyRepository#findByFarmBlockAndName}: a {@code null} farm
     * matches only the rows without a farm.
     *
     * @param farmId    farm identifier, or {@code null} for the rows without a farm
     * @param blockCode block code as stored, for example {@code "A"}
     * @return the matching rows, oldest first, possibly empty
     */
    @Query("""
            SELECT c FROM GrowthCalendar c
            WHERE ((CAST(:farmId AS Integer) IS NULL AND c.farmId IS NULL) OR c.farmId = :farmId)
              AND c.blockCode = :blockCode
            ORDER BY c.id ASC
            """)
    List<GrowthCalendar> findByFarmAndBlock(@Param("farmId") Integer farmId,
                                            @Param("blockCode") String blockCode);

    /**
     * Same rows as {@link #findByFarmAndBlock}, each locked until the end of
     * the transaction.
     *
     * <p>Reserved to the planting service: two plantings of the same block
     * update its calendar one after the other, so the older date is never
     * overwritten by a later one. Must run inside a transaction.
     *
     * @param farmId    farm identifier, or {@code null} for the rows without a farm
     * @param blockCode block code as stored, for example {@code "A"}
     * @return the matching rows, oldest first, locked, possibly empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM GrowthCalendar c
            WHERE ((:farmId IS NULL AND c.farmId IS NULL) OR c.farmId = :farmId)
              AND c.blockCode = :blockCode
            ORDER BY c.id ASC
            """)
    List<GrowthCalendar> findByFarmAndBlockForUpdate(@Param("farmId") Integer farmId,
                                                     @Param("blockCode") String blockCode);
}
