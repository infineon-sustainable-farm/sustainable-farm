package com.infineonbit.sustainablefarm.modules.farm.repository;

import com.infineonbit.sustainablefarm.modules.farm.entity.Block;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<Block, Long> {
    List<Block> findByFarmId(Long farmId);

    Page<Block> findByFarmId(Long farmId, Pageable pageable);

    Optional<Block> findByCode(String code);

    @EntityGraph(attributePaths = "farm")
    List<Block> findAllByIdIn(Collection<Long> ids);
}
