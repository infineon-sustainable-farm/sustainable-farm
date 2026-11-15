package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository
        extends JpaRepository<ShipmentEvent, Integer> {
}