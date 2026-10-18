package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.FertilizerProduct;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FertilizerProductRepository extends JpaRepository<FertilizerProduct, Long> {

    /** The whole catalogue, ordered by name. */
    List<FertilizerProduct> findAllByOrderByNameAsc();

    /**
     * Returns a fertilizer and locks its row until the end of the transaction.
     *
     * <p>Every application and loss starts here: a second one on the same
     * fertilizer waits for the first one to commit, then computes the stock
     * with its movement. Two of them sent at the same time can therefore never
     * take more than the stock. Must run inside a transaction.
     *
     * @param id the fertilizer identifier
     * @return the locked fertilizer, or empty if no fertilizer has this identifier
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM FertilizerProduct p WHERE p.id = :id")
    Optional<FertilizerProduct> findByIdForUpdate(@Param("id") Long id);
}
