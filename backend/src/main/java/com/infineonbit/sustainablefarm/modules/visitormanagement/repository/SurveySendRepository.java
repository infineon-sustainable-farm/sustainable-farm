package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveySend;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveySendRepository extends JpaRepository<SurveySend, Long> {

    List<SurveySend> findByVisitorId(Long visitorId);

    List<SurveySend> findByStatus(SurveyStatus status);
}