package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
     * @param allBlocks   {@code true} for every block, then {@code blockIds} is ignored
     * @param blockIds  ids of the blocks to keep, never empty ({@code service.PlantsBlocks.BlockFilter})
     * @return the matching varieties, ordered by block, then name, then id
     */
    @Query("""
            SELECT v FROM Variety v
            WHERE (:allBlocks = true OR v.blockId IN :blockIds)
            ORDER BY v.blockId ASC, v.name ASC, v.id ASC
            """)
    List<Variety> findByOptionalFilters(@Param("allBlocks") boolean allBlocks,
                                        @Param("blockIds") Collection<Long> blockIds);

    /**
     * Returns the variety rows of one (block, name) pair, oldest first. The
     * block gives the farm, so a row of one farm is never taken for a row of
     * another.
     *
     * <p>The name comparison ignores case, so "keitt" finds "Keitt".
     *
     * @param blockId   id of the block, a {@code farm.block} row
     * @param name      variety name, compared ignoring case
     * @return the matching rows, oldest first, possibly empty
     */
    @Query("""
            SELECT v FROM Variety v
            WHERE v.blockId = :blockId
              AND LOWER(v.name) = LOWER(:name)
            ORDER BY v.id ASC
            """)
    List<Variety> findByBlockAndName(@Param("blockId") Long blockId,
                                     @Param("name") String name);

    /**
     * Same rows as {@link #findByBlockAndName}, each locked until the end
     * of the transaction.
     *
     * <p>Reserved to the planting service: a second planting of an existing
     * row waits for the first one to commit, then sees its PLANTING. The shared
     * lookup stays without a lock, so the harvest service and the callers
     * outside a transaction keep working as before. Must run inside a
     * transaction.
     *
     * @param blockId   id of the block, a {@code farm.block} row
     * @param name      variety name, compared ignoring case
     * @return the matching rows, oldest first, locked, possibly empty
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT v FROM Variety v
            WHERE v.blockId = :blockId
              AND LOWER(v.name) = LOWER(:name)
            ORDER BY v.id ASC
            """)
    List<Variety> findByBlockAndNameForUpdate(@Param("blockId") Long blockId,
                                              @Param("name") String name);

    /**
     * The rows without a natural key, oldest first: rows written before
     * {@code variety_key} existed, for the startup filling.
     *
     * @return the rows whose {@code variety_key} is NULL
     */
    List<Variety> findByVarietyKeyIsNullOrderByIdAsc();

    /**
     * The row holding a natural key; there is at most one.
     *
     * @param varietyKey the natural key, see {@link Variety#keyOf}
     * @return the row, or empty if no row holds this key
     */
    Optional<Variety> findByVarietyKey(String varietyKey);

    /**
     * Writes the natural key of one row that has none yet.
     *
     * <p>A bulk update: it changes no other column, {@code date_maj} included,
     * and runs no entity callback. Must run inside a transaction.
     *
     * @param id         the row
     * @param varietyKey its natural key
     * @return the number of rows updated: 1, or 0 if the row already has a key
     */
    @Modifying
    @Query("UPDATE Variety v SET v.varietyKey = :varietyKey WHERE v.id = :id AND v.varietyKey IS NULL")
    int fillVarietyKey(@Param("id") Long id, @Param("varietyKey") String varietyKey);
}
