package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.PlantScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Planting scenarios. They are few and read whole: names are matched in Java,
 * ignoring case and accents, as for the variety references.
 */
@Repository
public interface PlantScenarioRepository extends JpaRepository<PlantScenario, Long> {
}
