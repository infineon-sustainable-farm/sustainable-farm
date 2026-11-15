package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A second guard against cross-site requests, on top of the SameSite=Strict
 * cookie: every write must come from one of the frontend origins listed in
 * APP_CORS_ORIGINS. Browsers always send Origin on such requests. The gate
 * scanner is not a browser and does not use the cookie, so a request with a
 * Bearer token and no session cookie is left to the access check.
 */
@Component
public class SiteSecurityOriginInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SiteSecurityOriginInterceptor.class);
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final Set<String> allowedOrigins;

    public SiteSecurityOriginInterceptor(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (READ_METHODS.contains(request.getMethod()) || CorsUtils.isPreFlightRequest(request)
                || isBearer(request.getHeader(HttpHeaders.AUTHORIZATION))) {
            return true;
        }
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && allowedOrigins.contains(origin)) {
            return true;
        }
        log.warn("Site Security write refused (origin not allowed): {} {}",
                request.getMethod(), request.getRequestURI());
        SiteSecurityAccessInterceptor.writeError(response, HttpStatus.FORBIDDEN,
                "This request must come from the Site Security app.");
        return false;
    }

    private static boolean isBearer(String authorization) {
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }
}
