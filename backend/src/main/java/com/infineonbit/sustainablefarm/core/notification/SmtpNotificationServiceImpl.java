package com.infineonbit.sustainablefarm.core.notification;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Real SMTP notification sink, active when {@code app.mail.enabled=true}.
 *
 * <p>Delivery failures are logged and swallowed so that a broken mail relay
 * never blocks the surrounding business operation (per the module rules).</p>
 */
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpNotificationServiceImpl(JavaMailSender mailSender,
                                       @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} (subject: {})", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} (subject: {}): {}", to, subject, e.getMessage());
        }
    }
}