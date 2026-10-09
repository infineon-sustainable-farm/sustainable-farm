package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.SalesChannel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesChannelRepository extends JpaRepository<SalesChannel, Integer> {
    // Basic CRUD (findAll, findById, save, deleteById...) comes free
    // from JpaRepository.
}