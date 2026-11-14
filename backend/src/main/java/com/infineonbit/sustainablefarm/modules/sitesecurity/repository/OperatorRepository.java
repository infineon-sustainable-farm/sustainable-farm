package com.infineonbit.sustainablefarm.modules.sitesecurity.repository;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatorRepository extends JpaRepository<OperatorEntity, String> {
    boolean existsByEmail(String email);

    /** Any account with this email, whatever its state. */
    Optional<OperatorEntity> findByEmail(String email);

    // Projections read before a row is locked: they load no entity, so the locked
    // read that follows always sees the database row (open-in-view keeps entities cached).

    @Query("select o.id from OperatorEntity o where o.email = :email")
    Optional<String> findIdByEmail(@Param("email") String email);

    @Query("select o.email from OperatorEntity o where o.id = :id")
    Optional<String> findEmailById(@Param("id") String id);

    @Query("select o.passwordHash from OperatorEntity o where o.id = :id")
    Optional<String> findPasswordHashById(@Param("id") String id);

    /** Locks the row so that concurrent requests cannot get around the sign-in code limits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperatorEntity o where o.email = :email and o.active = true")
    Optional<OperatorEntity> lockActiveByEmail(@Param("email") String email);

    /** Locks the operator being changed, so that two identical concurrent changes apply (and are audited) once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperatorEntity o where o.id = :id")
    Optional<OperatorEntity> lockById(@Param("id") String id);

    /** Locks the rows so that two concurrent changes cannot remove the last admin between them. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperatorEntity o where o.role = :role and o.active = true order by o.id")
    List<OperatorEntity> lockActiveByRole(@Param("role") SiteSecurityRole role);
}
