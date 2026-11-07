package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorSessionEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorSessionRepository extends JpaRepository<OperatorSessionEntity, String> {
    Optional<OperatorSessionEntity> findByTokenHash(String tokenHash);

    long countByOperatorIdAndRevokedAtIsNullAndExpiresAtAfter(String operatorId, Instant now);

    @Modifying
    @Query("update OperatorSessionEntity s set s.revokedAt = :now"
            + " where s.tokenHash = :tokenHash and s.revokedAt is null")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("update OperatorSessionEntity s set s.revokedAt = :now"
            + " where s.operatorId = :operatorId and s.revokedAt is null")
    int revokeAllForOperator(@Param("operatorId") String operatorId, @Param("now") Instant now);

    /** Removes the sessions of an operator that can no longer be used. */
    @Modifying
    @Query("delete from OperatorSessionEntity s where s.operatorId = :operatorId"
            + " and (s.expiresAt < :now or s.revokedAt is not null)")
    int deleteFinished(@Param("operatorId") String operatorId, @Param("now") Instant now);
}
