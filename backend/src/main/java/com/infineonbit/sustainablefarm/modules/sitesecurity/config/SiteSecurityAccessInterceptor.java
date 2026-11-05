package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Transitional, module-local access control for the Site Security API, until
 * the project adopts a shared authentication layer. It proves which role is
 * calling, not which person. It fails closed: a missing or too short token
 * configuration matches no request, so the API stays locked.
 * Any method that is not a read requires ADMIN, so new write endpoints are
 * protected by default.
 */
@Component
public class SiteSecurityAccessInterceptor implements HandlerInterceptor {

    public static final String ROLE_ATTRIBUTE = "siteSecurityRole";

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityAccessInterceptor.class);
    private static final int MIN_TOKEN_LENGTH = 32;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final byte[] readTokenDigest;
    private final byte[] adminTokenDigest;

    public SiteSecurityAccessInterceptor(
            @Value("${SITESECURITY_READ_TOKEN:}") String readToken,
            @Value("${SITESECURITY_ADMIN_TOKEN:}") String adminToken) {
        this.readTokenDigest = configuredDigest("SITESECURITY_READ_TOKEN", readToken);
        this.adminTokenDigest = configuredDigest("SITESECURITY_ADMIN_TOKEN", adminToken);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        SiteSecurityRole role = resolveRole(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (role == null) {
            log.warn("Site Security access denied (no valid token): {} {}",
                    request.getMethod(), request.getRequestURI());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            writeError(response, HttpStatus.UNAUTHORIZED, "A valid access token is required.");
            return false;
        }
        if (role != SiteSecurityRole.ADMIN && !READ_METHODS.contains(request.getMethod())) {
            log.warn("Site Security access denied (admin role required): {} {}",
                    request.getMethod(), request.getRequestURI());
            writeError(response, HttpStatus.FORBIDDEN, "This action requires the admin role.");
            return false;
        }

        request.setAttribute(ROLE_ATTRIBUTE, role);
        return true;
    }

    private SiteSecurityRole resolveRole(String authorization) {
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        byte[] presented = sha256(authorization.substring(BEARER_PREFIX.length()).trim());
        // Both comparisons always run, so timing does not reveal which token matched.
        boolean admin = matches(adminTokenDigest, presented);
        boolean read = matches(readTokenDigest, presented);
        if (admin) {
            return SiteSecurityRole.ADMIN;
        }
        return read ? SiteSecurityRole.READ : null;
    }

    private static boolean matches(byte[] expected, byte[] presented) {
        return expected != null && MessageDigest.isEqual(expected, presented);
    }

    private static byte[] configuredDigest(String variable, String token) {
        if (token == null || token.trim().length() < MIN_TOKEN_LENGTH) {
            log.warn("{} is missing or shorter than {} characters: this role is disabled.",
                    variable, MIN_TOKEN_LENGTH);
            return null;
        }
        return sha256(token.trim());
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // The message is a constant from this class, so it needs no JSON escaping.
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}");
    }
}
