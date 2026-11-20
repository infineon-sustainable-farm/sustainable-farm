package com.infineonbit.sustainablefarm.modules.machinery.repository;

import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByName(String name);

    boolean existsByCode(String code);

    Optional<Equipment> findByCode(String code);

    List<Equipment> findByStageInOrderByIdAsc(Collection<Stage> stages);

    @Query("SELECT COALESCE(MAX(e.id), 0) FROM Equipment e")
    long findMaxId();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE Equipment e SET e.status = :status WHERE e.id = :id")
    int updateEquipmentStatusById(@Param("id")Long id,@Param("status") Status status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Equipment e WHERE e.id = :id")
    int deleteEquipmentById(@Param("id") Long id);
}
