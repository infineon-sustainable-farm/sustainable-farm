package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.visitor LEFT JOIN FETCH f.surveySend "
            + "WHERE f.visitor.id = :visitorId")
    List<Feedback> findByVisitorId(@Param("visitorId") Long visitorId);

    boolean existsByVisitorId(Long visitorId);

    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.visitor LEFT JOIN FETCH f.surveySend "
            + "WHERE f.submittedAt BETWEEN :from AND :to")
    List<Feedback> findBySubmittedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT f FROM Feedback f LEFT JOIN FETCH f.visitor LEFT JOIN FETCH f.surveySend "
            + "ORDER BY f.submittedAt DESC")
    List<Feedback> findAllByOrderBySubmittedAtDesc();

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.submittedAt BETWEEN :from AND :to")
    long countBySubmittedAtBetween(@Param("from") Instant from, @Param("to") Instant to);
}