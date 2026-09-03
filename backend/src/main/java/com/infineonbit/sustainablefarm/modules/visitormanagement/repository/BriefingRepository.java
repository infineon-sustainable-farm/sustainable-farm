package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Briefing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BriefingRepository extends JpaRepository<Briefing, Long> {

    Optional<Briefing> findByRegistrationId(Long registrationId);
}
