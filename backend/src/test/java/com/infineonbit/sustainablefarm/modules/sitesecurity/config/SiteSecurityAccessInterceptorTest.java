package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SiteSecurityAccessInterceptorTest {

    private static final String GATE = "g".repeat(32);
    private static final String READER_COOKIE = "reader-cookie";
    private static final String ADMIN_COOKIE = "admin-cookie";

    private final SessionService sessions = mock(SessionService.class);

    /** Stands for a controller: one scan endpoint and one ordinary endpoint. */
    static class FakeController {
        @GateScanner
        public void scan() {
        }

        public void zones() {
        }
    }

    private SiteSecurityAccessInterceptor interceptor(String gateToken) {
        when(sessions.resolve(anyString())).thenReturn(Optional.empty());
        when(sessions.resolve(READER_COOKIE)).thenReturn(Optional.of(
                new SiteSecurityPrincipal("r", "reader@example.test", SiteSecurityRole.READ)));
        when(sessions.resolve(ADMIN_COOKIE)).thenReturn(Optional.of(
                new SiteSecurityPrincipal("a", "admin@example.test", SiteSecurityRole.ADMIN)));
        return new SiteSecurityAccessInterceptor(
                new StaticListableBeanFactory(Map.of("sessions", sessions)).getBeanProvider(SessionService.class),
                gateToken);
    }

    private static HandlerMethod handler(String name) throws NoSuchMethodException {
        return new HandlerMethod(new FakeController(), FakeController.class.getMethod(name));
    }

    private static MockHttpServletRequest request(String method, String cookie, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/sitesecurity/zones");
        if (cookie != null) {
            request.setCookies(new Cookie(SessionService.COOKIE_NAME, cookie));
        }
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    private int status(SiteSecurityAccessInterceptor interceptor, MockHttpServletRequest request, Object handler)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, handler);
        return response.getStatus();
    }

    @Test
    void shouldRefuse_withoutASession() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);

        assertEquals(401, status(interceptor, request("GET", null, null), handler("zones")));
        assertEquals(401, status(interceptor, request("GET", "unknown", null), handler("zones")));
        assertFalse(interceptor.preHandle(request("GET", null, null), new MockHttpServletResponse(), handler("zones")));
    }

    @Test
    void shouldRequireAdmin_forEveryNonReadMethod() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);

        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            assertEquals(403, status(interceptor, request(method, READER_COOKIE, null), handler("zones")), method);
            assertEquals(200, status(interceptor, request(method, ADMIN_COOKIE, null), handler("zones")), method);
        }
    }

    @Test
    void shouldExposeTheCaller_whenTheSessionIsValid() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);
        MockHttpServletRequest request = request("POST", ADMIN_COOKIE, null);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), handler("zones")));
        assertEquals(SiteSecurityRole.ADMIN, request.getAttribute(SiteSecurityAccessInterceptor.ROLE_ATTRIBUTE));
        SiteSecurityPrincipal principal =
                (SiteSecurityPrincipal) request.getAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE);
        assertEquals("admin@example.test", principal.actor());
    }

    @Test
    void theOldReadAndAdminTokens_shouldOpenNothing() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);

        assertEquals(401, status(interceptor, request("GET", null, "Bearer " + "r".repeat(32)), handler("zones")));
        assertEquals(401, status(interceptor, request("GET", null, "Bearer " + "a".repeat(32)), handler("zones")));
    }

    @Test
    void gateToken_shouldOnlyOpenTheScannerEndpoints() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);

        assertEquals(200, status(interceptor, request("POST", null, "Bearer " + GATE), handler("scan")));
        assertEquals(200, status(interceptor, request("POST", null, "bearer " + GATE), handler("scan")));
        assertEquals(403, status(interceptor, request("GET", null, "Bearer " + GATE), handler("zones")));
        assertEquals(403, status(interceptor, request("POST", null, "Bearer " + GATE), handler("zones")));
        assertEquals(403, status(interceptor, request("GET", null, "Bearer " + GATE), new Object()));
        assertEquals(401, status(interceptor, request("POST", null, "Bearer wrong"), handler("scan")));
        assertEquals(401, status(interceptor, request("POST", null, "Basic " + GATE), handler("scan")));
    }

    @Test
    void scannerEndpoints_shouldStayClosedToReaders() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);

        assertEquals(403, status(interceptor, request("POST", READER_COOKIE, null), handler("scan")));
    }

    @Test
    void gateScanner_shouldBeDisabled_whenItsTokenIsMissingOrTooShort() throws Exception {
        assertEquals(401, status(interceptor(""), request("POST", null, "Bearer "), handler("scan")));
        String shortToken = "g".repeat(31);
        assertEquals(401, status(interceptor(shortToken), request("POST", null, "Bearer " + shortToken), handler("scan")));
    }

    @Test
    void anExpiredCookie_shouldNotFallBackToTheGateToken() throws Exception {
        SiteSecurityAccessInterceptor interceptor = interceptor(GATE);

        assertEquals(401, status(interceptor, request("POST", "expired", "Bearer " + GATE), handler("scan")));
    }
}
