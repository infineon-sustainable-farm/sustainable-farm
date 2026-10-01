package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.AgriActivity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Data access for paid agritourism activities.
 */
public interface AgriActivityRepository extends JpaRepository<AgriActivity, Long> {

    List<AgriActivity> findAllByOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgriActivity a WHERE a.id = :id")
    Optional<AgriActivity> findByIdForUpdate(@Param("id") Long id);
}