package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.notification.api.AlertBoard;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDetails;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertDto;
import com.infineonbit.sustainablefarm.modules.notification.api.AlertSeverity;
import com.infineonbit.sustainablefarm.modules.notification.api.SourceModule;
import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emitting the module's automatic alerts (out-of-range quality, probable leak,
 * irrigation postponed because of rain, maintenance to schedule).
 *
 * <p>The alerts are kept in the single alert store of the notification module (ADR 0004),
 * where the module's notification center reads them. They do not come from any logged-in user:
 * they are attached to the technical IoT system person ({@link SystemUsers#iotSystemUserId()}),
 * since they must be visible in the notification center until the global platform reassigns
 * them.</p>
 */
@Service
public class AlertService {

    /** The module of the water supply alerts in the alert store. */
    public static final SourceModule SOURCE = SourceModule.WATERSUPPLY;

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    /** Anti-duplicate window: the same alert is not repeated twice within this interval. */
    private static final Duration DEDUPLICATION_WINDOW = Duration.ofHours(12);

    private final AlertBoard alertBoard;
    private final SystemUsers systemUsers;

    public AlertService(AlertBoard alertBoard, SystemUsers systemUsers) {
        this.alertBoard = alertBoard;
        this.systemUsers = systemUsers;
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
    public AlertDto raise(String type, String title, String message, String actionUrl) {
        AlertDto saved = alertBoard.raise(SOURCE, details(type, title, message, actionUrl));
        // Operational trace: without it, an automatic alert left no trace
        // in the logs, while it is often the only visible sign of an incident.
        log.info("Alert raised [{}] {} (action: {})", type, title, actionUrl);
        return saved;
    }

    /**
     * Anti-duplicate variant used for repetitive sensor measurements (an abnormal
     * consumption every minute must not generate an alert per measurement): no alert while an
     * unread one with the same title was raised within the window.
     *
     * @return the created alert, or an {@link Optional#empty()} when an identical active alert already exists
     */
    @Transactional
    public Optional<AlertDto> raiseOnce(String type, String title, String message, String actionUrl) {
        Optional<AlertDto> saved = alertBoard.raiseOnce(SOURCE, details(type, title, message, actionUrl),
                DEDUPLICATION_WINDOW);
        saved.ifPresent(alert -> log.info("Alert raised [{}] {} (action: {})", type, title, actionUrl));
        return saved;
    }

    private AlertDetails details(String type, String title, String message, String actionUrl) {
        return AlertDetails.builder()
                .type(type)
                .severity(severityOf(type))
                .title(title)
                .message(message)
                .recipientPersonId(systemUsers.iotSystemUserId())
                .read(false)
                .actionUrl(actionUrl)
                .build();
    }

    /**
     * The severity of a water supply alert: its type when the type is a level (info, warning,
     * critical, any case), none otherwise.
     */
    public static AlertSeverity severityOf(String type) {
        if (type == null) {
            return null;
        }
        String level = type.trim().toUpperCase(Locale.ROOT);
        for (AlertSeverity severity : AlertSeverity.values()) {
            if (severity.name().equals(level)) {
                return severity;
            }
        }
        return null;
    }
}
