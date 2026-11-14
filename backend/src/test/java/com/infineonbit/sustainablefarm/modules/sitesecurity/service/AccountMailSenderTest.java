package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountMailSender.Outcome;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountMailSender.Result;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountMailSenderTest {

    /** Stand-in for the mail library's SMTP exception: only getReturnCode() matters. */
    static class SmtpLikeException extends jakarta.mail.MessagingException {
        private final int code;

        SmtpLikeException(int code) {
            super("smtp " + code);
            this.code = code;
        }

        public int getReturnCode() {
            return code;
        }
    }

    @SuppressWarnings("unchecked")
    private static AccountMailSender sender(JavaMailSender mail, boolean enabled, String... profiles) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mail);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return new AccountMailSender(provider, mock(NotificationService.class), environment, enabled,
                "Farm Security <noreply@farm.example>");
    }

    private static JavaMailSender mailMock() {
        JavaMailSender mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        return mail;
    }

    @Test
    void submitted_shouldCarryOurMessageIdAndDeliveryHeader() throws Exception {
        JavaMailSender mail = mailMock();
        Result result = sender(mail, true).send("delivery-1", "ada@example.test", "Subject", "<p>Hi</p>");

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail).send(sent.capture());
        assertEquals(Outcome.SUBMITTED, result.outcome());
        assertEquals("<delivery-1@farm.example>", result.messageId());
        assertEquals("<delivery-1@farm.example>", sent.getValue().getHeader("Message-ID")[0]);
        assertEquals("delivery-1", sent.getValue().getHeader(AccountMailSender.DELIVERY_HEADER)[0]);
        assertEquals("ada@example.test", sent.getValue().getAllRecipients()[0].toString());
    }

    @Test
    void smtp5xx_shouldBeAPermanentFailure() {
        JavaMailSender mail = mailMock();
        doThrow(new MailSendException("rejected", new SmtpLikeException(550))).when(mail).send(any(MimeMessage.class));

        Result result = sender(mail, true).send("d", "x@example.test", "S", "B");

        assertEquals(Outcome.PERMANENT_FAILURE, result.outcome());
        assertEquals("SMTP_5XX", result.errorClass());
    }

    @Test
    void smtp4xx_shouldBeATemporaryFailure() {
        JavaMailSender mail = mailMock();
        doThrow(new MailSendException(Map.of(new Object(), new SmtpLikeException(451)))).when(mail).send(any(MimeMessage.class));

        Result result = sender(mail, true).send("d", "x@example.test", "S", "B");

        assertEquals(Outcome.TRANSIENT_FAILURE, result.outcome());
        assertEquals("SMTP_4XX", result.errorClass());
    }

    @Test
    void connectionProblem_shouldBeATemporaryFailure() {
        JavaMailSender mail = mailMock();
        doThrow(new MailSendException("connect timed out")).when(mail).send(any(MimeMessage.class));

        assertEquals(Outcome.TRANSIENT_FAILURE, sender(mail, true).send("d", "x@example.test", "S", "B").outcome());
    }

    @Test
    void refusedCredentials_shouldBeAPermanentFailure() {
        JavaMailSender mail = mailMock();
        doThrow(new MailAuthenticationException("bad credentials")).when(mail).send(any(MimeMessage.class));

        Result result = sender(mail, true).send("d", "x@example.test", "S", "B");

        assertEquals(Outcome.PERMANENT_FAILURE, result.outcome());
        assertEquals("AUTH", result.errorClass());
    }

    @Test
    void mailDisabled_shouldSendNothing_andSaySo() {
        JavaMailSender mail = mailMock();

        Result result = sender(mail, false).send("d", "x@example.test", "S", "B");

        assertEquals(Outcome.MAIL_DISABLED, result.outcome());
        assertNull(result.messageId());
        verify(mail, never()).send(any(MimeMessage.class));
    }
}
