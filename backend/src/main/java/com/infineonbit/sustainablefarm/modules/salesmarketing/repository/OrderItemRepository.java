package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    // Basic CRUD comes from JpaRepository.
    // Add custom finder methods here as needed.
}
