package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RainwaterHarvestRepository extends JpaRepository<RainwaterHarvest, UUID> {
    Page<RainwaterHarvest> findBySourceId(UUID sourceId, Pageable pageable);

    /**
     * Volume d'eau de pluie recupere sur un intervalle semi-ouvert [start, end[ : calcule par la
     * base au lieu d'un parcours complet de la table cote application.
     */
    @Query("select coalesce(sum(h.harvestedLiters), 0) from RainwaterHarvest h "
            + "where h.captureDate >= :start and h.captureDate < :end")
    double sumHarvestedLitersBetween(Instant start, Instant end);
}
