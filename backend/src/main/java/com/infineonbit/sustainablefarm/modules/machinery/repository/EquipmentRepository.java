package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByName(String name);
}
