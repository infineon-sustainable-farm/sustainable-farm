package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for paid agritourism activities.
 */
public interface AgriActivityRepository extends JpaRepository<AgriActivity, Long> {

    List<AgriActivity> findAllByOrderByNameAsc();
}