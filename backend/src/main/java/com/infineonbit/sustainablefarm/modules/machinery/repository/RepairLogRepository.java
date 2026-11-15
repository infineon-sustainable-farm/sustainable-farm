package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.RepairLog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RepairLogRepository extends JpaRepository<RepairLog, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM RepairLog r WHERE r.id = :id")
    int deleteRepairLogById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE RepairLog r SET r.equipment = null WHERE r.equipment.id = :equipmentId")
    int detachAllFromEquipment(@Param("equipmentId") Long equipmentId);
}
