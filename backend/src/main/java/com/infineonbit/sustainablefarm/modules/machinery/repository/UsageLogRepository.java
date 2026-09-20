package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.UsageLog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM UsageLog u WHERE u.id = :id")
    int deleteUsageLogById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE UsageLog u SET u.equipment = null WHERE u.equipment.id = :equipmentId")
    int detachAllFromEquipment(@Param("equipmentId") Long equipmentId);
}
