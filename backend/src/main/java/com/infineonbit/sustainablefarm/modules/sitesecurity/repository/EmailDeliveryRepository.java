package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.EmailDeliveryEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailDeliveryRepository extends JpaRepository<EmailDeliveryEntity, String> {

    Optional<EmailDeliveryEntity> findFirstByAccountIdOrderByCreatedAtDesc(String accountId);

    Optional<EmailDeliveryEntity> findFirstByAccountIdAndKindInOrderByCreatedAtDesc(String accountId, List<String> kinds);

    List<EmailDeliveryEntity> findByAccountIdOrderByCreatedAtDesc(String accountId);

    @Query("select d from EmailDeliveryEntity d where d.submissionStatus = 'RETRY_SCHEDULED'"
            + " and d.nextAttemptAt <= :now order by d.nextAttemptAt")
    List<EmailDeliveryEntity> findDueRetries(@Param("now") Instant now);

    List<EmailDeliveryEntity> findByKindInAndDeliveryStatus(List<String> kinds, String deliveryStatus);

    /** Emails interrupted before an outcome was recorded (still QUEUED or SENDING long after creation). */
    @Query("select d from EmailDeliveryEntity d where d.submissionStatus in ('QUEUED', 'SENDING')"
            + " and coalesce(d.lastAttemptAt, d.createdAt) < :before")
    List<EmailDeliveryEntity> findStuck(@Param("before") Instant before);
}
