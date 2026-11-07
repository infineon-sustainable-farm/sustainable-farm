package com.infineonbit.sustainablefarm.modules.sitesecurity;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorSessionEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Opens real sessions for tests, without going through the email code. */
public final class TestSessions {

    /** One of the default APP_CORS_ORIGINS, so writes pass the origin check. */
    public static final String APP_ORIGIN = "http://localhost:5173";

    private TestSessions() {
    }

    /** Creates an active operator with that email and role, and returns a cookie value for it. */
    public static String open(OperatorRepository operators, OperatorSessionRepository sessions,
            String email, SiteSecurityRole role) {
        OperatorEntity operator = operators.findAll().stream()
                .filter(existing -> existing.getEmail().equals(email))
                .findFirst()
                .orElseGet(() -> operators.save(OperatorEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .email(email)
                        .role(role)
                        .active(true)
                        .createdBy("test")
                        .createdAt(Instant.now())
                        .build()));
        return openFor(sessions, operator.getId(), Duration.ofHours(8));
    }

    /** A session for an existing operator, ending after the given duration (negative: already ended). */
    public static String openFor(OperatorSessionRepository sessions, String operatorId, Duration validFor) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        Instant now = Instant.now();
        sessions.save(OperatorSessionEntity.builder()
                .id(UUID.randomUUID().toString())
                .operatorId(operatorId)
                .tokenHash(SessionService.hash(token))
                .createdAt(now)
                .expiresAt(now.plus(validFor))
                .build());
        return token;
    }

    /** Sends the session cookie and the app's Origin header, as the browser does. */
    public static RequestPostProcessor signedIn(String token) {
        return request -> {
            request.setCookies(new Cookie(SessionService.COOKIE_NAME, token));
            request.addHeader(HttpHeaders.ORIGIN, APP_ORIGIN);
            return request;
        };
    }
}
