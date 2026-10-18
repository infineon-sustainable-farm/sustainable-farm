package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.MaintenanceSchedule;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {

    @Modifying
    @Transactional
    @Query("DELETE FROM MaintenanceSchedule m WHERE m.id = :id")
    int deleteMaintenanceScheduleById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE MaintenanceSchedule m SET m.equipment = null WHERE m.equipment.id = :equipmentId")
    int detachAllFromEquipment(@Param("equipmentId") Long equipmentId);
}
