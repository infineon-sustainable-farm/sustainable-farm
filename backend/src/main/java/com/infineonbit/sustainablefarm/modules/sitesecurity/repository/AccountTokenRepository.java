package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, String> {

    Optional<AccountTokenEntity> findByTokenHash(String tokenHash);

    /** Only the account id: nothing is loaded into the persistence context before the rows are locked. */
    @Query("select t.accountId from AccountTokenEntity t where t.tokenHash = :tokenHash")
    Optional<String> findAccountIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountTokenEntity t where t.tokenHash = :tokenHash")
    Optional<AccountTokenEntity> lockByTokenHash(@Param("tokenHash") String tokenHash);

    List<AccountTokenEntity> findByAccountIdAndPurposeOrderByCreatedAtDesc(String accountId, String purpose);

    long countByAccountIdAndPurposeAndCreatedAtAfter(String accountId, String purpose, Instant since);

    /** Makes every open link of this purpose unusable (resend, revoke, disable, reset). */
    @Modifying
    @Query("update AccountTokenEntity t set t.revokedAt = :now where t.accountId = :accountId"
            + " and t.purpose = :purpose and t.usedAt is null and t.revokedAt is null")
    int revokeOpen(@Param("accountId") String accountId, @Param("purpose") String purpose, @Param("now") Instant now);

    @Modifying
    @Query("update AccountTokenEntity t set t.revokedAt = :now where t.id = :id and t.usedAt is null and t.revokedAt is null")
    int revokeOne(@Param("id") String id, @Param("now") Instant now);

    @Modifying
    @Query("update AccountTokenEntity t set t.revokedAt = :now where t.accountId = :accountId"
            + " and t.usedAt is null and t.revokedAt is null")
    int revokeAllOpen(@Param("accountId") String accountId, @Param("now") Instant now);

    /** Single use, enforced by the database: only one caller can turn an open token into a used one. */
    @Modifying
    @Query("update AccountTokenEntity t set t.usedAt = :now where t.id = :id"
            + " and t.usedAt is null and t.revokedAt is null and t.expiresAt > :now")
    int markUsed(@Param("id") String id, @Param("now") Instant now);

    @Modifying
    @Query("update AccountTokenEntity t set t.confirmedAt = :now where t.id = :id and t.confirmedAt is null")
    int markConfirmed(@Param("id") String id, @Param("now") Instant now);
}
