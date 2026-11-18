package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnergyComponentRepository extends JpaRepository<EnergyComponent, Long> {

    /** The component with this business code (ENG-001...). */
    Optional<EnergyComponent> findByCode(String code);

    boolean existsByCode(String code);
}
