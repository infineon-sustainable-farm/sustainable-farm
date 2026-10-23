package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {
}