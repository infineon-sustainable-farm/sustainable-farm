package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.SparePart;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    boolean existsByName(String name);

    Optional<SparePart> findById(Long id);

    List<SparePart> findByEquipmentId(Long equipmentId);

    @Query("SELECT s FROM SparePart s WHERE s.quantity <= s.reorderThreshold ORDER BY s.quantity ASC, s.name ASC")
    List<SparePart> findLowStockSpareParts();

    @Modifying
    @Transactional
    @Query("DELETE FROM SparePart s WHERE s.id = :id")
    int deleteSparePartById(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE SparePart s SET s.equipment = null WHERE s.equipment.id = :equipmentId")
    int detachAllFromEquipment(@Param("equipmentId") Long equipmentId);

    @Modifying
    @Transactional
    @Query("UPDATE SparePart s SET s.quantity = :quantity WHERE s.id = :id")
    int updateSparePartQuantityById(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Modifying
    @Transactional
    @Query("UPDATE SparePart s SET s.unitCost = :unitCost WHERE s.id = :id")
    int updateSparePartUnitCostById(@Param("id") Long id, @Param("unitCost") java.math.BigDecimal unitCost);
}
