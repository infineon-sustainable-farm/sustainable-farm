package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import jakarta.mail.internet.MimeMessage;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Hands one account email to the mail provider through the application's
 * shared mail sender (the shared notification service is not changed). It sets
 * our own Message-ID and an X-Farm-Security-Delivery header, so that provider
 * delivery and bounce events can later be matched to the delivery row.
 *
 * <p>"Submitted" means only that the provider's SMTP server accepted the message.</p>
 */
@Component
public class AccountMailSender {

    public static final String DELIVERY_HEADER = "X-Farm-Security-Delivery";

    private static final Logger log = LoggerFactory.getLogger(AccountMailSender.class);

    /** Outcome of one attempt. {@code transientFailure} decides whether a retry makes sense. */
    public record Result(Outcome outcome, String messageId, String errorClass) {
        static Result submitted(String messageId) {
            return new Result(Outcome.SUBMITTED, messageId, null);
        }
    }

    public enum Outcome { SUBMITTED, LOCAL_CONSOLE, MAIL_DISABLED, TRANSIENT_FAILURE, PERMANENT_FAILURE }

    private final ObjectProvider<JavaMailSender> mailSender;
    private final NotificationService notificationService;
    private final boolean mailEnabled;
    private final boolean devProfile;
    private final String from;

    public AccountMailSender(ObjectProvider<JavaMailSender> mailSender, NotificationService notificationService,
            Environment environment, @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:farm-security@localhost}") String from) {
        this.mailSender = mailSender;
        this.notificationService = notificationService;
        this.mailEnabled = mailEnabled;
        this.devProfile = environment.acceptsProfiles(Profiles.of("dev"));
        this.from = from;
    }

    public Result send(String deliveryId, String to, String subject, String html) {
        if (!mailEnabled) {
            if (devProfile) {
                // Development only: the console sink prints the email, link included.
                notificationService.send(to, subject, html);
                return new Result(Outcome.LOCAL_CONSOLE, null, "MAIL_DISABLED");
            }
            log.warn("MAIL_ENABLED is false: account email {} was not sent.", deliveryId);
            return new Result(Outcome.MAIL_DISABLED, null, "MAIL_DISABLED");
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.error("No mail sender is configured: account email {} was not sent.", deliveryId);
            return new Result(Outcome.PERMANENT_FAILURE, null, "NO_MAIL_SENDER");
        }
        String messageId = "<" + deliveryId + "@" + domainOf(from) + ">";
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            message.setHeader(DELIVERY_HEADER, deliveryId);
            // Spring's sender keeps a Message-ID set before sending.
            message.setHeader("Message-ID", messageId);
            sender.send(message);
            log.info("Account email {} submitted to the mail provider", deliveryId);
            return Result.submitted(messageId);
        } catch (MailAuthenticationException e) {
            log.error("Account email {}: the mail provider refused the credentials", deliveryId);
            return new Result(Outcome.PERMANENT_FAILURE, messageId, "AUTH");
        } catch (MailException e) {
            Integer code = smtpCode(e);
            if (code != null && code >= 500) {
                log.warn("Account email {}: rejected by the mail provider (SMTP {})", deliveryId, code);
                return new Result(Outcome.PERMANENT_FAILURE, messageId, "SMTP_5XX");
            }
            String error = code != null ? "SMTP_4XX" : (e instanceof MailSendException ? "CONNECTION" : "MAIL_ERROR");
            log.warn("Account email {}: temporary failure ({})", deliveryId, error);
            return new Result(Outcome.TRANSIENT_FAILURE, messageId, error);
        } catch (Exception e) {
            log.warn("Account email {}: could not be built or sent ({})", deliveryId, e.getClass().getSimpleName());
            return new Result(Outcome.TRANSIENT_FAILURE, messageId, "MAIL_ERROR");
        }
    }

    /** The SMTP reply code carried by the mail library's exception, if any (no compile-time dependency on it). */
    static Integer smtpCode(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < 10; depth++) {
            Integer code = returnCode(current);
            if (code != null) {
                return code;
            }
            if (current instanceof MailSendException send) {
                for (Exception nested : send.getFailedMessages().values()) {
                    Integer nestedCode = smtpCode(nested);
                    if (nestedCode != null) {
                        return nestedCode;
                    }
                }
            }
            if (current instanceof jakarta.mail.MessagingException messaging && messaging.getNextException() != null
                    && messaging.getNextException() != current) {
                Integer nextCode = returnCode(messaging.getNextException());
                if (nextCode != null) {
                    return nextCode;
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private static Integer returnCode(Throwable error) {
        try {
            Method method = error.getClass().getMethod("getReturnCode");
            Object value = method.invoke(error);
            if (value instanceof Integer code && code >= 200 && code < 600) {
                return code;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not an SMTP exception.
        }
        return null;
    }

    private static String domainOf(String address) {
        int at = address == null ? -1 : address.lastIndexOf('@');
        String domain = at < 0 ? "farm-security.local" : address.substring(at + 1).replaceAll("[^A-Za-z0-9.-]", "");
        return domain.isEmpty() ? "farm-security.local" : domain;
    }
}
