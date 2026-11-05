package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiteSecurityAccessInterceptorTest {

    private static final String READ = "r".repeat(32);
    private static final String ADMIN = "a".repeat(32);

    private MockHttpServletResponse call(SiteSecurityAccessInterceptor interceptor, String method, String header)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/sitesecurity/zones");
        if (header != null) {
            request.addHeader("Authorization", header);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());
        return response;
    }

    @Test
    void shouldStayLocked_whenNoTokenIsConfigured() throws Exception {
        SiteSecurityAccessInterceptor interceptor = new SiteSecurityAccessInterceptor("", "");

        assertEquals(401, call(interceptor, "GET", null).getStatus());
        assertEquals(401, call(interceptor, "GET", "Bearer ").getStatus());
        assertEquals(401, call(interceptor, "GET", "Bearer " + ADMIN).getStatus());
    }

    @Test
    void shouldDisableARole_whenItsTokenIsTooShort() throws Exception {
        String shortToken = "a".repeat(31);
        SiteSecurityAccessInterceptor interceptor = new SiteSecurityAccessInterceptor(READ, shortToken);

        assertEquals(401, call(interceptor, "POST", "Bearer " + shortToken).getStatus());
    }

    @Test
    void shouldRejectOtherAuthenticationSchemes() throws Exception {
        SiteSecurityAccessInterceptor interceptor = new SiteSecurityAccessInterceptor(READ, ADMIN);

        assertEquals(401, call(interceptor, "GET", "Basic " + ADMIN).getStatus());
        assertEquals(401, call(interceptor, "GET", ADMIN).getStatus());
    }

    @Test
    void shouldRequireAdmin_forEveryNonReadMethod() throws Exception {
        SiteSecurityAccessInterceptor interceptor = new SiteSecurityAccessInterceptor(READ, ADMIN);

        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"}) {
            assertEquals(403, call(interceptor, method, "Bearer " + READ).getStatus(), method);
        }
    }

    @Test
    void shouldExposeTheRole_whenTheTokenIsValid() throws Exception {
        SiteSecurityAccessInterceptor interceptor = new SiteSecurityAccessInterceptor(READ, ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/sitesecurity/credentials");
        request.addHeader("Authorization", "bearer " + ADMIN);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(SiteSecurityRole.ADMIN, request.getAttribute(SiteSecurityAccessInterceptor.ROLE_ATTRIBUTE));
    }

    @Test
    void shouldNotContinue_whenAccessIsDenied() throws Exception {
        SiteSecurityAccessInterceptor interceptor = new SiteSecurityAccessInterceptor(READ, ADMIN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sitesecurity/logs");

        assertFalse(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
