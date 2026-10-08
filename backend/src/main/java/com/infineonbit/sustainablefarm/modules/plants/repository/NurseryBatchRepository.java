package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.NurseryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NurseryBatchRepository extends JpaRepository<NurseryBatch, Long> {

    /**
     * Returns the batches of one farm that have this code, oldest first.
     *
     * <p>A {@code null} farm is a value here, not a missing filter: it matches
     * only the batches without a farm, as in
     * {@link VarietyRepository#findByFarmBlockAndName}. The code must match
     * exactly; it is stored normalized ({@code "P1"}).
     *
     * @param farmId    farm identifier, or {@code null} for the batches without a farm
     * @param batchCode batch code as stored, for example {@code "P1"}
     * @return the matching batches, possibly empty
     */
    @Query("""
            SELECT b FROM NurseryBatch b
            WHERE ((CAST(:farmId AS Integer) IS NULL AND b.farmId IS NULL) OR b.farmId = :farmId)
              AND b.batchCode = :batchCode
            ORDER BY b.id ASC
            """)
    List<NurseryBatch> findByFarmAndCode(@Param("farmId") Integer farmId,
                                         @Param("batchCode") String batchCode);

    /**
     * Returns the batches of one farm, or of every farm.
     *
     * <p>A {@code null} farm disables the filter, as for the other lists of the
     * module. The order uses no nullable column, so it is the same on every
     * database.
     *
     * @param farmId farm identifier, or {@code null} for every farm
     * @return the batches, ordered by start date then identifier
     */
    @Query("""
            SELECT b FROM NurseryBatch b
            WHERE (CAST(:farmId AS Integer) IS NULL OR b.farmId = :farmId)
            ORDER BY b.startedOn ASC, b.id ASC
            """)
    List<NurseryBatch> findByOptionalFarm(@Param("farmId") Integer farmId);

    /**
     * Returns a batch and locks its row until the end of the transaction.
     *
     * <p>Every write to a batch starts here: a second write to the same batch
     * waits for the first one to commit, then reads its events. Two losses or
     * transplants sent at the same time can therefore never take more plants
     * than the batch has. Must run inside a transaction.
     *
     * @param id the batch identifier
     * @return the locked batch, or empty if no batch has this identifier
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM NurseryBatch b WHERE b.id = :id")
    Optional<NurseryBatch> findByIdForUpdate(@Param("id") Long id);
}
