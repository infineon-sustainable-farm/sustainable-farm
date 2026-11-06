package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import org.springframework.web.util.HtmlUtils;

/**
 * Builds the emails sent to a credential holder. The subjects are fixed because
 * the mail layer logs them, so they must never contain personal data.
 */
final class CredentialEmailBuilder {

    record MailContent(String subject, String html) {
    }

    private CredentialEmailBuilder() {
    }

    static MailContent build(CredentialNotificationEvent.Type type, CredentialUserEntity credential, String zoneName) {
        return switch (type) {
            case GRANTED -> new MailContent("Votre accès au site a été accordé", html(
                    "Accès accordé", "Un accès au site vous a été accordé.", credential, zoneName));
            case REVOKED -> new MailContent("Votre accès au site a été révoqué", html(
                    "Accès révoqué", "Votre accès au site a été révoqué et n'est plus valable.", credential, zoneName));
        };
    }

    private static String html(String title, String intro, CredentialUserEntity credential, String zoneName) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;\">"
                + "<h2>" + HtmlUtils.htmlEscape(title) + "</h2>"
                + "<p>" + HtmlUtils.htmlEscape(intro) + "</p>"
                + "<table style=\"border-collapse:collapse;font-size:14px;\">"
                + row("Nom", credential.getName())
                + row("Niveau d'accès", credential.getLevel())
                + row("Zone", zoneName)
                + row("Date de validité", credential.getValidUntil())
                + "</table></div>";
    }

    private static String row(String label, String value) {
        return "<tr><td style=\"padding:4px 16px 4px 0;color:#475569;\">" + HtmlUtils.htmlEscape(label)
                + "</td><td style=\"padding:4px 0;\">" + HtmlUtils.htmlEscape(value) + "</td></tr>";
    }
}
