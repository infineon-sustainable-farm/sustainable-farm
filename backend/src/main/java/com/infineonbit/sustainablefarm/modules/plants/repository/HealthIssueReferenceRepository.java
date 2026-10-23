package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HealthIssueReferenceRepository extends JpaRepository<HealthIssueReference, Long> {

    /** The catalogue row of a code, compared exactly: codes are stored upper case. */
    Optional<HealthIssueReference> findByCode(String code);
}
