package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveySend;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurveySendRepository extends JpaRepository<SurveySend, Long> {

    @Query("SELECT s FROM SurveySend s JOIN FETCH s.visitor LEFT JOIN FETCH s.feedback "
            + "WHERE s.visitor.id = :visitorId")
    List<SurveySend> findByVisitorId(@Param("visitorId") Long visitorId);

    boolean existsByVisitorId(Long visitorId);

    @Query("SELECT s FROM SurveySend s JOIN FETCH s.visitor LEFT JOIN FETCH s.feedback "
            + "WHERE s.status = :status")
    List<SurveySend> findByStatus(@Param("status") SurveyStatus status);

    @Query("SELECT s FROM SurveySend s JOIN FETCH s.visitor LEFT JOIN FETCH s.feedback")
    List<SurveySend> findAllWithRefs();
}