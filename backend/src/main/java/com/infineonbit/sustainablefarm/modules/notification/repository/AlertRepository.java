package com.infineonbit.sustainablefarm.modules.notification.repository;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertStatus;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import com.infineonbit.sustainablefarm.modules.notification.entity.Alert;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByIdAndSource(Long id, SourceModule source);

    List<Alert> findBySourceOrderByIdAsc(SourceModule source);

    List<Alert> findBySourceAndStatusOrderByIdAsc(SourceModule source, AlertStatus status);

    Page<Alert> findBySource(SourceModule source, Pageable pageable);

    Page<Alert> findBySourceAndRecipientPersonId(SourceModule source, Long recipientPersonId, Pageable pageable);

    long countBySource(SourceModule source);

    long countBySourceAndTypeIgnoreCase(SourceModule source, String type);

    boolean existsBySourceAndTitleAndReadIsFalseAndRaisedAtAfter(SourceModule source, String title, Instant threshold);

    boolean existsBySourceAndRelatedEntityTypeAndRelatedEntityId(SourceModule source, String type, String id);
}
