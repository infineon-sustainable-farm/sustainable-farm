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

    /** Locks the row so that concurrent requests cannot get around the sign-in code limits. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperatorEntity o where o.email = :email and o.active = true")
    Optional<OperatorEntity> lockActiveByEmail(@Param("email") String email);

    /** Locks the rows so that two concurrent changes cannot remove the last admin between them. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperatorEntity o where o.role = :role and o.active = true")
    List<OperatorEntity> lockActiveByRole(@Param("role") SiteSecurityRole role);
}
