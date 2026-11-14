package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialNotificationStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CredentialUserRepository extends JpaRepository<CredentialUserEntity, String> {
    boolean existsByName(String name);

    /** Locks the credential being revoked, so that two concurrent revocations apply (and are audited) once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CredentialUserEntity c where c.id = :id")
    Optional<CredentialUserEntity> lockById(@Param("id") String id);

    /** Writes this column only: saving the whole row could undo a concurrent revocation. */
    @Transactional
    @Modifying
    @Query("update CredentialUserEntity c set c.notificationStatus = :status where c.id = :id")
    void updateNotificationStatus(@Param("id") String id, @Param("status") CredentialNotificationStatus status);

    /** Skips a revoked credential: its status now belongs to the revocation email. */
    @Transactional
    @Modifying
    @Query("update CredentialUserEntity c set c.notificationStatus = :status "
            + "where c.id = :id and c.status <> 'Revoked'")
    void updateGrantNotificationStatus(@Param("id") String id, @Param("status") CredentialNotificationStatus status);
}
