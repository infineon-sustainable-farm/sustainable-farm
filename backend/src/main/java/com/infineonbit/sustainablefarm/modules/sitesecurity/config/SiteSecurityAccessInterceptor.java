package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SiteSecurityAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Access control for the Site Security API. An operator is supplied by the
 * central identity adapter; the gate scanner by SITESECURITY_GATE_TOKEN, which only
 * opens the endpoints marked {@link GateScanner}. The operator's role is read
 * again on every request. Any method that is not a read requires ADMIN, so new
 * write endpoints are protected by default. It fails closed.
 */
@Component
public class SiteSecurityAccessInterceptor implements HandlerInterceptor {

    public static final String ROLE_ATTRIBUTE = "siteSecurityRole";
    public static final String PRINCIPAL_ATTRIBUTE = "siteSecurityPrincipal";

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityAccessInterceptor.class);
    private static final int MIN_TOKEN_LENGTH = 32;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final SiteSecurityAuthorizationService authorizationService;
    private final byte[] gateTokenDigest;

    /**
     * The identity provider is injected so production can fail closed until the
     * central platform adapter is configured.
     */
    public SiteSecurityAccessInterceptor(SiteSecurityAuthorizationService authorizationService,
            @Value("${SITESECURITY_GATE_TOKEN:}") String gateToken) {
        this.authorizationService = authorizationService;
        this.gateTokenDigest = configuredDigest(gateToken);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        Optional<SiteSecurityPrincipal> caller = resolve(request);
        if (caller.isEmpty()) {
            log.warn("Site Security access denied (central identity unavailable): {} {}",
                    request.getMethod(), request.getRequestURI());
            writeError(response, HttpStatus.UNAUTHORIZED, "Sign in to use Site Security.");
            return false;
        }
        SiteSecurityPrincipal principal = caller.get();
        if (!isAllowed(principal, request, handler)) {
            log.warn("Site Security access denied ({} role): {} {}",
                    principal.role(), request.getMethod(), request.getRequestURI());
            writeError(response, HttpStatus.FORBIDDEN, principal.role() == SiteSecurityRole.GATE
                    ? "The gate token cannot be used here."
                    : "This action requires the admin role or a permission you do not have.");
            return false;
        }

        request.setAttribute(ROLE_ATTRIBUTE, principal.role());
        request.setAttribute(PRINCIPAL_ATTRIBUTE, principal);
        return true;
    }

    /** The central identity wins; a browser cannot use the gate token as an account. */
    private Optional<SiteSecurityPrincipal> resolve(HttpServletRequest request) {
        if (isGateToken(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            return Optional.of(SiteSecurityPrincipal.gateScanner());
        }
        return authorizationService.currentPrincipal();
    }

    /**
     * Decided from the matched controller, because a URL can be encoded in several ways.
     * Admins may call everything. Users are denied by default: only endpoints marked
     * {@link AnyAccount}, or {@link UserPermission} with a permission the user holds
     * (reads only), are open to them; {@link AdminOnly} classes never are.
     */
    static boolean isAllowed(SiteSecurityPrincipal principal, HttpServletRequest request, Object handler) {
        HandlerMethod method = handler instanceof HandlerMethod found ? found : null;
        SiteSecurityRole role = principal.role();
        if (role == SiteSecurityRole.GATE) {
            return method != null && (method.hasMethodAnnotation(GateScanner.class)
                    || method.getBeanType().isAnnotationPresent(GateScanner.class));
        }
        if (role == SiteSecurityRole.ADMIN) {
            return true;
        }
        if (method == null || method.getBeanType().isAnnotationPresent(AdminOnly.class)) {
            return false;
        }
        if (method.hasMethodAnnotation(AnyAccount.class) || method.getBeanType().isAnnotationPresent(AnyAccount.class)) {
            return true;
        }
        UserPermission permission = method.getMethodAnnotation(UserPermission.class);
        return permission != null && READ_METHODS.contains(request.getMethod()) && principal.can(permission.value());
    }

    private boolean isGateToken(String authorization) {
        if (gateTokenDigest == null || authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return false;
        }
        byte[] presented = sha256(authorization.substring(BEARER_PREFIX.length()).trim());
        return MessageDigest.isEqual(gateTokenDigest, presented);
    }

    private static byte[] configuredDigest(String token) {
        if (token == null || token.trim().length() < MIN_TOKEN_LENGTH) {
            log.info("SITESECURITY_GATE_TOKEN is missing or shorter than {} characters: the gate scanner is disabled.",
                    MIN_TOKEN_LENGTH);
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

    static void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // The message is a constant from this package, so it needs no JSON escaping.
        response.getWriter().write("{\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message + "\"}");
    }
}
