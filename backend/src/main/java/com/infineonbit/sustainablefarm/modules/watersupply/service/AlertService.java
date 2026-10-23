package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emitting the module's automatic alerts (out-of-range quality, probable leak,
 * irrigation postponed because of rain, maintenance to schedule).
 *
 * <p>These alerts do not come from any logged-in user: they are attached to the technical
 * account {@link SystemUsers#IOT_SYSTEM_USER_ID}, since they must be visible in the
 * notification center until the global platform reassigns them.</p>
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    /** Anti-duplicate window: the same alert is not repeated twice within this interval. */
    private static final Duration DEDUPLICATION_WINDOW = Duration.ofHours(12);

    private final NotificationRepository notificationRepository;

    public AlertService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Creates an alert (notification) aimed at the module's technical account.
     *
     * @param type     alert level/type: info, warning, critical
     * @param title    short title
     * @param message  detailed message displayed in the notification center
     * @param actionUrl application route to open to handle the alert (nullable)
     */
    @Transactional
    public Notification raise(String type, String title, String message, String actionUrl) {
        Notification notification = new Notification();
        notification.setUserId(SystemUsers.IOT_SYSTEM_USER_ID);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setActionUrl(actionUrl);
        Notification saved = notificationRepository.save(notification);
        // Operational trace: without it, an automatic alert left no trace
        // in the logs, while it is often the only visible sign of an incident.
        log.info("Alert raised [{}] {} (action: {})", type, title, actionUrl);
        return saved;
    }

    /**
     * Anti-duplicate variant used for repetitive sensor measurements (an abnormal
     * consumption every minute must not generate an alert per measurement).
     *
     * @return the created alert, or an {@link Optional#empty()} when an identical active alert already exists
     */
    @Transactional
    public Optional<Notification> raiseOnce(String type, String title, String message, String actionUrl) {
        Instant threshold = Instant.now().minus(DEDUPLICATION_WINDOW);
        // Check in the database (title + alert still open + recent window): no more full
        // table scan at every sensor measurement, and the ranking stays deterministic.
        boolean alreadyRaised = notificationRepository
                .existsByTitleAndReadIsFalseAndCreatedAtAfter(title, threshold);
        if (alreadyRaised) {
            return Optional.empty();
        }
        return Optional.of(raise(type, title, message, actionUrl));
    }
}
