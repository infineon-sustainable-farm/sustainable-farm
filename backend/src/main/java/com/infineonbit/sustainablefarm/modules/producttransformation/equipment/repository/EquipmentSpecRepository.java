package com.infineonbit.sustainablefarm.modules.producttransformation.equipment.repository;

import com.infineonbit.sustainablefarm.modules.producttransformation.equipment.model.EquipmentSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository of the product transformation attributes of the register equipment.
 */
@Repository
public interface EquipmentSpecRepository extends JpaRepository<EquipmentSpec, Long> {

    Optional<EquipmentSpec> findByEquipmentId(Long equipmentId);

    List<EquipmentSpec> findByEquipmentIdIn(Collection<Long> equipmentIds);

    void deleteByEquipmentId(Long equipmentId);
}
