package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.EmailKind;
import org.springframework.web.util.HtmlUtils;

/**
 * Text of the account emails. Subjects are fixed (no names in headers). Names
 * are HTML-escaped. Links come from {@link AccountLinks} only.
 */
final class AccountEmailBuilder {

    record MailContent(String subject, String html) {
        @Override
        public String toString() {
            return "MailContent[subject=" + subject + "]";
        }
    }

    private AccountEmailBuilder() {
    }

    static MailContent build(EmailKind kind, String fullName, String link, String extra) {
        String hello = "<p>Bonjour" + (fullName == null || fullName.isBlank() ? "" : " " + HtmlUtils.htmlEscape(fullName)) + ",</p>";
        return switch (kind) {
            case INVITATION -> new MailContent("Invitation à Farm Security",
                    wrap(hello
                            + "<p>Un administrateur vous a créé un compte Farm Security.</p>"
                            + "<p>Pour l'activer, ouvrez ce lien et choisissez votre mot de passe :</p>"
                            + button(link, "Activer mon compte")
                            + "<p>Ce lien est personnel, ne sert qu'une fois et expire dans 7 jours.</p>"
                            + "<p>Si vous n'attendiez pas cette invitation, ignorez cet email.</p>"));
            case PASSWORD_SETUP -> new MailContent("Définissez votre mot de passe Farm Security",
                    wrap(hello
                            + "<p>Farm Security passe à la connexion par mot de passe. Ouvrez ce lien pour choisir le vôtre :</p>"
                            + button(link, "Choisir mon mot de passe")
                            + "<p>Ce lien est personnel, ne sert qu'une fois et expire dans 7 jours.</p>"));
            case PASSWORD_RESET -> new MailContent("Réinitialisation de votre mot de passe Farm Security",
                    wrap(hello
                            + "<p>Une réinitialisation de votre mot de passe a été demandée. Pour choisir un nouveau mot de passe :</p>"
                            + button(link, "Choisir un nouveau mot de passe")
                            + "<p>Ce lien ne sert qu'une fois et expire dans 30 minutes.</p>"
                            + "<p>Si vous n'avez rien demandé, ignorez cet email : votre mot de passe reste inchangé.</p>"));
            case PASSWORD_CHANGED -> new MailContent("Votre mot de passe Farm Security a été modifié",
                    wrap(hello
                            + "<p>Le mot de passe de votre compte Farm Security vient d'être modifié, et vos autres sessions ont été fermées.</p>"
                            + "<p>Si ce n'était pas vous, prévenez immédiatement un administrateur.</p>"));
            case ACTIVATION_NOTICE -> new MailContent("Un compte Farm Security a été activé",
                    wrap(hello
                            + "<p>Le compte invité " + HtmlUtils.htmlEscape(extra == null ? "" : extra)
                            + " vient d'être activé. Si cette activation est inattendue, désactivez le compte depuis l'écran Utilisateurs.</p>"
                            + (link == null ? "" : button(link, "Ouvrir Farm Security"))));
        };
    }

    private static String wrap(String body) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;\">"
                + "<h2>Farm Security</h2>" + body + "</div>";
    }

    private static String button(String link, String label) {
        String safe = HtmlUtils.htmlEscape(link);
        return "<p><a href=\"" + safe + "\" style=\"display:inline-block;padding:10px 16px;background:#0b7a6e;"
                + "color:#fff;text-decoration:none;border-radius:8px;\">" + label + "</a></p>"
                + "<p style=\"font-size:12px;color:#555;\">Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>"
                + safe + "</p>";
    }
}
