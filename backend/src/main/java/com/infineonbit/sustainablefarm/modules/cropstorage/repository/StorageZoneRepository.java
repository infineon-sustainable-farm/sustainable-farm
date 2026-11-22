package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StorageZoneRepository extends JpaRepository<StorageZone, Long> {

    /**
     * Reads a zone and locks its row until the surrounding transaction ends
     * (SELECT ... FOR UPDATE). Every write that fills a zone (API-005 entry,
     * IN or TRANSFER movement) takes this lock before checking the remaining
     * capacity, so two concurrent writes on the same zone are serialized and
     * cannot both pass the check. Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select z from StorageZone z where z.id = :id")
    Optional<StorageZone> findForUpdateById(@Param("id") Long id);
}
