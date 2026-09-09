package com.infineonbit.sustainablefarm.core.notification;

/**
 * Outbound notification gateway shared across modules.
 *
 * <p>Two implementations are available: the SMTP one (active when
 * {@code app.mail.enabled=true}) and a console one (default, dev mode)
 * that just logs the message instead of sending it.</p>
 */
public interface NotificationService {

    /**
     * Sends an email. Implementations must never throw: a delivery failure
     * is logged and must not block the surrounding business transaction.
     */
    void send(String to, String subject, String htmlBody);
}