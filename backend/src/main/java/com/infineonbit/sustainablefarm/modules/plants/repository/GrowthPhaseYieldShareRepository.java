package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Yield share of each growth phase, one row per phase, read whole. */
@Repository
public interface GrowthPhaseYieldShareRepository extends JpaRepository<GrowthPhaseYieldShare, Long> {
}
