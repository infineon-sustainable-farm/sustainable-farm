package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityOperatorEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Site Security operator assignments, one per person of the shared directory. */
@Repository
public interface SiteSecurityOperatorRepository extends JpaRepository<SiteSecurityOperatorEntity, String> {
    Optional<SiteSecurityOperatorEntity> findByPersonId(Long personId);
}
