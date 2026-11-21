package com.infineonbit.sustainablefarm.modules.farm.repository;

import com.infineonbit.sustainablefarm.modules.farm.entity.Zone;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmZoneRepository extends JpaRepository<Zone, Long> {
    @EntityGraph(attributePaths = "block")
    List<Zone> findByBlockId(Long blockId);

    Page<Zone> findByBlockId(Long blockId, Pageable pageable);

    @EntityGraph(attributePaths = "block")
    List<Zone> findByBlockFarmId(Long farmId);

    @EntityGraph(attributePaths = "block")
    List<Zone> findByBlockCodeIsNotNull();

    @EntityGraph(attributePaths = "block")
    List<Zone> findAllByIdIn(java.util.Collection<Long> ids);

    @Override
    @EntityGraph(attributePaths = "block")
    List<Zone> findAll();
}
