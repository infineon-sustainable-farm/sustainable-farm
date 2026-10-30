package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeneratorEventRepository extends JpaRepository<GeneratorEvent, String> {
}
