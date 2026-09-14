package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByVisitorId(Long visitorId);

    List<Feedback> findBySubmittedAtBetween(Instant from, Instant to);

    List<Feedback> findAllByOrderBySubmittedAtDesc();

    long countBySubmittedAtBetween(Instant from, Instant to);
}