package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GeneratorEventRepository extends JpaRepository<GeneratorEvent, String> {

    List<GeneratorEvent> findAllByOrderByDateDesc();

    @Query("""
            select coalesce(sum(event.durationHours), 0) as durationHours,
                   coalesce(sum(event.fuelLiters), 0) as fuelLiters
            from GeneratorEvent event
            where event.date >= :startDate and event.date < :endDate
            """)
    GeneratorEventMonthlyTotals sumMonthlyTotals(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    interface GeneratorEventMonthlyTotals {
        BigDecimal getDurationHours();

        BigDecimal getFuelLiters();
    }
}
