package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LogFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Applies the access control to this module's API only: other modules are not affected. */
@Configuration
@RequiredArgsConstructor
public class SiteSecurityWebConfig implements WebMvcConfigurer {

    private static final String API = "/api/v1/sitesecurity/**";

    private final SiteSecurityOriginInterceptor originInterceptor;
    private final SiteSecurityAccessInterceptor accessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // The origin check also covers sign-in and sign-out.
        registry.addInterceptor(originInterceptor).addPathPatterns(API).order(0);
        registry.addInterceptor(accessInterceptor)
                .addPathPatterns(API)
                // Public sign-in endpoints, each by its exact path (never a wildcard).
                .excludePathPatterns("/api/v1/sitesecurity/auth/request-code",
                        "/api/v1/sitesecurity/auth/verify",
                        "/api/v1/sitesecurity/auth/logout",
                        "/api/v1/sitesecurity/auth/login",
                        "/api/v1/sitesecurity/auth/tokens/inspect",
                        "/api/v1/sitesecurity/auth/invitations/confirm",
                        "/api/v1/sitesecurity/auth/activate",
                        "/api/v1/sitesecurity/auth/forgot-password",
                        "/api/v1/sitesecurity/auth/reset-password")
                .order(1);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, LogFilter.class, LogFilter::fromParam);
    }
}
