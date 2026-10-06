package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	Page<Notification> findByUserId(UUID userId, Pageable pageable);

	/** Number of alerts of a given type (dashboard "anomalies" KPI), computed in the database. */
	long countByTypeIgnoreCase(String type);

	/**
	 * True when the same alert is already open and recent: this is the deduplication of the
	 * repetitive sensor alerts, checked in the database rather than by a full application-side scan.
	 */
	boolean existsByTitleAndReadIsFalseAndCreatedAtAfter(String title, Instant threshold);
}
