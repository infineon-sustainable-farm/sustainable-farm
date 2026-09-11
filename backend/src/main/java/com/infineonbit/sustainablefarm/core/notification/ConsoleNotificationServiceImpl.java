package com.infineonbit.sustainablefarm.core.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Dev-mode notification sink: logs emails to the console when no SMTP
 * server is configured ({@code app.mail.enabled=false}).
 */
@Service
@ConditionalOnMissingBean(SmtpNotificationServiceImpl.class)
public class ConsoleNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationServiceImpl.class);

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("\n----[DEMO EMAIL]----\nto: {}\nsubject: {}\n{}\n--------------------",
                to, subject, htmlBody);
    }
}