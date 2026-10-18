package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Corrections of the yield share of the growth phases, at most one row per phase, read whole. */
@Repository
public interface GrowthPhaseYieldShareRepository extends JpaRepository<GrowthPhaseYieldShare, Long> {

    /**
     * The correction of a phase.
     *
     * @param growthPhase the phase label, such as {@code "gradual production"}
     * @return its row, or empty while the phase keeps its default
     */
    Optional<GrowthPhaseYieldShare> findByGrowthPhase(String growthPhase);
}
