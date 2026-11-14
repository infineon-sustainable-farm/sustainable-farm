package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SignInFailureEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SignInFailureRepository extends JpaRepository<SignInFailureEntity, String> {

    long countByClientKeyAndOccurredAtAfter(String clientKey, Instant since);

    long countByEmailKeyAndOccurredAtAfter(String emailKey, Instant since);

    long countByEmailKeyAndClientKeyAndOccurredAtAfter(String emailKey, String clientKey, Instant since);

    long countByDeviceIdAndOccurredAtAfter(String deviceId, Instant since);

    @Query("select count(distinct f.clientKey) from SignInFailureEntity f where f.emailKey = :emailKey and f.occurredAt > :since")
    long countClientsForEmailSince(@Param("emailKey") String emailKey, @Param("since") Instant since);

    @Query("select max(f.occurredAt) from SignInFailureEntity f where f.emailKey = :emailKey and f.clientKey = :clientKey"
            + " and f.occurredAt > :since")
    Optional<Instant> lastForEmailAndClient(@Param("emailKey") String emailKey, @Param("clientKey") String clientKey,
            @Param("since") Instant since);

    @Query("select min(f.occurredAt) from SignInFailureEntity f where f.clientKey = :clientKey and f.occurredAt > :since")
    Optional<Instant> firstForClient(@Param("clientKey") String clientKey, @Param("since") Instant since);

    @Query("select min(f.occurredAt) from SignInFailureEntity f where f.emailKey = :emailKey and f.occurredAt > :since")
    Optional<Instant> firstForEmail(@Param("emailKey") String emailKey, @Param("since") Instant since);

    /** A successful sign-in clears that client's progressive delay for this email. */
    @Modifying
    @Query("delete from SignInFailureEntity f where f.emailKey = :emailKey and f.clientKey = :clientKey")
    int deleteForEmailAndClient(@Param("emailKey") String emailKey, @Param("clientKey") String clientKey);

    /** A successful password reset proves ownership: every limit on this email is lifted. */
    @Modifying
    @Query("delete from SignInFailureEntity f where f.emailKey = :emailKey")
    int deleteForEmail(@Param("emailKey") String emailKey);

    @Modifying
    @Query("delete from SignInFailureEntity f where f.occurredAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
