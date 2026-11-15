package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricingHistoryRepository extends JpaRepository<PricingHistory, Integer> {
}