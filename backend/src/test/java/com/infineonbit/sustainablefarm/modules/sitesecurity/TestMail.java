package com.infineonbit.sustainablefarm.modules.sitesecurity;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reads the emails the application hands to a mocked JavaMailSender. */
public final class TestMail {

    private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");
    private static final Pattern CODE = Pattern.compile("<strong>(\\d+)</strong>");

    private TestMail() {
    }

    public static void prepare(JavaMailSender mail) {
        reset(mail);
        when(mail.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    public static void clear(JavaMailSender mail) {
        clearInvocations(mail);
    }

    /** All messages sent so far (since the last clear). */
    public static List<MimeMessage> sent(JavaMailSender mail) {
        List<MimeMessage> messages = new ArrayList<>();
        mockingDetails(mail).getInvocations().stream()
                .filter(i -> i.getMethod().getName().equals("send") && i.getArguments().length == 1
                        && i.getArguments()[0] instanceof MimeMessage)
                .forEach(i -> messages.add((MimeMessage) i.getArguments()[0]));
        return messages;
    }

    /** Waits for at least n messages to this address and returns the newest. */
    public static MimeMessage await(JavaMailSender mail, String to, int count) {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            List<MimeMessage> found = to(mail, to);
            if (found.size() >= count) {
                return found.get(found.size() - 1);
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        verify(mail, timeout(1).atLeast(count)).send(any(MimeMessage.class));
        throw new AssertionError("No email to " + to);
    }

    public static List<MimeMessage> to(JavaMailSender mail, String to) {
        return sent(mail).stream().filter(m -> recipient(m).equalsIgnoreCase(to)).toList();
    }

    public static String recipient(MimeMessage message) {
        try {
            return message.getAllRecipients()[0].toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String subject(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (Exception e) {
            return "";
        }
    }

    public static String html(MimeMessage message) {
        try {
            return text(message);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String token(MimeMessage message) {
        Matcher matcher = TOKEN.matcher(html(message));
        if (!matcher.find()) {
            throw new AssertionError("No link in the email");
        }
        return matcher.group(1);
    }

    public static String code(MimeMessage message) {
        Matcher matcher = CODE.matcher(html(message));
        if (!matcher.find()) {
            throw new AssertionError("No code in the email");
        }
        return matcher.group(1);
    }

    private static String text(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                all.append(text(multipart.getBodyPart(i)));
            }
            return all.toString();
        }
        return "";
    }
}
