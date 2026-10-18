package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Agronomic reference of the varieties. It holds a handful of rows, read whole:
 * names are matched in Java, ignoring case and accents, not in SQL.
 */
@Repository
public interface VarietyReferenceRepository extends JpaRepository<VarietyReference, Long> {
}
