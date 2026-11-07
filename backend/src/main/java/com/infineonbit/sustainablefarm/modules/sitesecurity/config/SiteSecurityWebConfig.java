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

    private final SiteSecurityAccessInterceptor accessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor)
                .addPathPatterns("/api/v1/sitesecurity/**")
                .excludePathPatterns("/api/v1/sitesecurity/auth/request-code", "/api/v1/sitesecurity/auth/verify");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, LogFilter.class, LogFilter::fromParam);
    }
}
