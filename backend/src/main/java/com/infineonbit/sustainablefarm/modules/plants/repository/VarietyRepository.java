package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VarietyRepository extends JpaRepository<Variety, Long> {

    /**
     * Returns the varieties matching the given filters.
     *
     * <p>Both parameters are optional: a {@code null} parameter disables its own
     * filter, so the same query serves "all varieties", "one farm", "one block"
     * and "one block of one farm". A filter that matches nothing yields an empty
     * list, never an error.
     *
     * @param farmId    farm identifier, or {@code null} to ignore the farm
     * @param blockCode raw block value as stored (for example {@code "A"}),
     *                  or {@code null} to ignore the block
     * @return the matching varieties, ordered by block then name
     */
    @Query("""
            SELECT v FROM Variety v
            WHERE (:farmId IS NULL OR v.farmId = :farmId)
              AND (:blockCode IS NULL OR v.blockCode = :blockCode)
            ORDER BY v.blockCode ASC, v.name ASC
            """)
    List<Variety> findByOptionalFilters(@Param("farmId") Integer farmId,
                                        @Param("blockCode") String blockCode);
}
