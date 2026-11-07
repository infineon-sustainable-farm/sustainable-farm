package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LoginCodeEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginCodeRepository extends JpaRepository<LoginCodeEntity, String> {

    long countByOperatorIdAndCreatedAtAfter(String operatorId, Instant since);

    /** Locks the code, so that concurrent guesses are all counted and it is used only once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LoginCodeEntity c where c.consumedAt is null and c.operatorId in "
            + "(select o.id from OperatorEntity o where o.email = :email and o.active = true)")
    List<LoginCodeEntity> lockUnusedByActiveOperatorEmail(@Param("email") String email);

    @Modifying
    @Query("update LoginCodeEntity c set c.consumedAt = :now "
            + "where c.operatorId = :operatorId and c.consumedAt is null")
    void consumeUnused(@Param("operatorId") String operatorId, @Param("now") Instant now);

    @Modifying
    @Query("delete from LoginCodeEntity c where c.operatorId = :operatorId and c.createdAt < :before")
    void deleteOlderThan(@Param("operatorId") String operatorId, @Param("before") Instant before);
}
