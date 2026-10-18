package com.infineonbit.sustainablefarm.core.notification;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void smtpImplementer_sendsThroughJavaMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
        SmtpNotificationServiceImpl service = new SmtpNotificationServiceImpl(
                mailSender, "visits@sustainable-farm.local");

        assertThat(service.send("lucas@example.com", "Rappel", "<p>Demain</p>")).isTrue();

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void smtpImplementer_reportsDeliveryFailure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
        org.mockito.Mockito.doThrow(new RuntimeException("connection refused"))
                .when(mailSender).send(any(MimeMessage.class));
        SmtpNotificationServiceImpl service = new SmtpNotificationServiceImpl(
                mailSender, "visits@sustainable-farm.local");

        assertThat(service.send("lucas@example.com", "Rappel", "<p>Demain</p>")).isFalse();
    }

    @Test
    void consoleImplementer_reportsSuccess() {
        ConsoleNotificationServiceImpl service = new ConsoleNotificationServiceImpl();

        assertThat(service.send("lucas@example.com", "Rappel", "<p>Demain</p>")).isTrue();
    }
}