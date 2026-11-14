package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds the links put in account emails. The base is SITESECURITY_PUBLIC_URL;
 * until it is set, the first allowed app origin (APP_CORS_ORIGINS) is used,
 * which is right for local and test stacks only. A link is never built from a
 * request's Host header. The token goes in the URL fragment, which browsers do
 * not send to servers, so it stays out of access logs and Referer headers.
 */
@Component
public class AccountLinks {

    private static final Logger log = LoggerFactory.getLogger(AccountLinks.class);

    private final String base;

    public AccountLinks(@Value("${SITESECURITY_PUBLIC_URL:}") String publicUrl,
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        String chosen = publicUrl == null || publicUrl.isBlank()
                ? allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).findFirst().orElse("")
                : publicUrl.trim();
        this.base = validated(chosen);
        if (publicUrl == null || publicUrl.isBlank()) {
            log.warn("SITESECURITY_PUBLIC_URL is not set: account email links use {}. Set an HTTPS address before release.",
                    base.isEmpty() ? "(none)" : base);
        } else if (!base.startsWith("https://")) {
            log.warn("SITESECURITY_PUBLIC_URL is not HTTPS: account email links are not safe outside a test stack.");
        }
    }

    public boolean isConfigured() {
        return !base.isEmpty();
    }

    public String activation(String token) {
        return base + "/sitesecurity/activate#token=" + token;
    }

    public String passwordReset(String token) {
        return base + "/sitesecurity/reset-password#token=" + token;
    }

    public String signIn() {
        return base + "/sitesecurity";
    }

    private static String validated(String value) {
        if (value.isEmpty()) {
            return "";
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("https") || scheme.equals("http")) || uri.getHost() == null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                log.error("The public address for account emails is not a plain http(s) origin: links are disabled.");
                return "";
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            boolean local = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]");
            if (scheme.equals("http") && !local) {
                // Links carry account secrets: plain HTTP only on this machine (test stacks).
                log.error("The public address for account emails must use HTTPS: links are disabled.");
                return "";
            }
            String text = value;
            while (text.endsWith("/")) {
                text = text.substring(0, text.length() - 1);
            }
            return text;
        } catch (IllegalArgumentException e) {
            log.error("The public address for account emails is not a valid URL: links are disabled.");
            return "";
        }
    }
}
