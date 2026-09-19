package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Workshop;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.WorkshopStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    List<Workshop> findByStatus(WorkshopStatus status);
}