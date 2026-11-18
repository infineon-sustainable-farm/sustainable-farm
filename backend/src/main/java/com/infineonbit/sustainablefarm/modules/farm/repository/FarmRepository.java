package com.infineonbit.sustainablefarm.modules.farm.repository;

import com.infineonbit.sustainablefarm.modules.farm.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, Long> {
}
