package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.FuelLog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FuelLogRepository extends JpaRepository<FuelLog, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM FuelLog f WHERE f.id = :id")
    int deleteFuelLogById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE FuelLog f SET f.equipment = null WHERE f.equipment.id = :equipmentId")
    int detachAllFromEquipment(@Param("equipmentId") Long equipmentId);
}
