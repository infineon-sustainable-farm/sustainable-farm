package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.FailedSignInEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedSignInRepository extends JpaRepository<FailedSignInEntity, String> {

    /** Wrong email codes (rows without a kind predate passwords and were codes). */
    @Query("select count(f) from FailedSignInEntity f where f.operatorId = :operatorId"
            + " and f.occurredAt > :since and (f.kind is null or f.kind = 'CODE')")
    long countCodeFailuresSince(@Param("operatorId") String operatorId, @Param("since") Instant since);

    @Query("select count(f) from FailedSignInEntity f where f.operatorId = :operatorId"
            + " and f.occurredAt > :since and f.kind = 'PASSWORD'")
    long countPasswordFailuresSince(@Param("operatorId") String operatorId, @Param("since") Instant since);

    @Modifying
    @Query("delete from FailedSignInEntity f where f.operatorId = :operatorId and f.occurredAt < :before")
    void deleteOlderThan(@Param("operatorId") String operatorId, @Param("before") Instant before);

    /** A successful password reset lifts the password sign-in block. */
    @Modifying
    @Query("delete from FailedSignInEntity f where f.operatorId = :operatorId and f.kind = 'PASSWORD'")
    void deletePasswordFailures(@Param("operatorId") String operatorId);
}
