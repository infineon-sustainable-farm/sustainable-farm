package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.TrustedDeviceEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, String> {

    Optional<TrustedDeviceEntity> findByTokenHash(String tokenHash);

    List<TrustedDeviceEntity> findByAccountIdAndRevokedAtIsNullOrderByCreatedAtDesc(String accountId);

    @Modifying
    @Query("update TrustedDeviceEntity d set d.revokedAt = :now where d.accountId = :accountId and d.revokedAt is null")
    int revokeAll(@Param("accountId") String accountId, @Param("now") Instant now);

    @Modifying
    @Query("update TrustedDeviceEntity d set d.revokedAt = :now where d.accountId = :accountId and d.revokedAt is null"
            + " and d.tokenHash <> :keepHash")
    int revokeAllExcept(@Param("accountId") String accountId, @Param("keepHash") String keepHash, @Param("now") Instant now);

    @Modifying
    @Query("delete from TrustedDeviceEntity d where d.expiresAt < :before or d.revokedAt < :before")
    int deleteFinishedBefore(@Param("before") Instant before);
}
