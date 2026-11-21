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

    private static final String API = "/api/site-security/**";

    private final SiteSecurityOriginInterceptor originInterceptor;
    private final SiteSecurityAccessInterceptor accessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // The origin check applies to all module endpoints.
        registry.addInterceptor(originInterceptor).addPathPatterns(API).order(0);
        registry.addInterceptor(accessInterceptor)
                .addPathPatterns(API)
                .order(1);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, LogFilter.class, LogFilter::fromParam);
    }
}
