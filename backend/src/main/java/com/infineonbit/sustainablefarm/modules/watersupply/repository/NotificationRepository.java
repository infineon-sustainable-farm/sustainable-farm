package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	Page<Notification> findByUserId(UUID userId, Pageable pageable);

	/** Nombre d'alertes d'un type donne (KPI « anomalies » du dashboard), calcule par la base. */
	long countByTypeIgnoreCase(String type);

	/**
	 * Vrai si la meme alerte est deja ouverte et recente : c'est la deduplication des alertes
	 * repetitives des capteurs, verifiee en base plutot que par un parcours complet cote application.
	 */
	boolean existsByTitleAndReadIsFalseAndCreatedAtAfter(String title, Instant threshold);
}
