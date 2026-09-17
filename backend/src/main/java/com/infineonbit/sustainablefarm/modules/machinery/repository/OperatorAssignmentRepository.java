package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.OperatorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorAssignmentRepository extends JpaRepository<OperatorAssignment, Long> {
    boolean existsByEquipment_Id(Long equipmentId);
}