package com.infineonbit.sustainablefarm.modules.producttransformation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Product Transformation HTTP security gate.
 *
 * <p>The module has no authentication infrastructure yet (no JWT, no OAuth2, no
 * login form). Gating {@code /api/**} by role without any way to obtain a role
 * makes every endpoint answer 403, which bricked the dev UI during the Day 13
 * visual validation. The two chains below keep the production posture (routes
 * closed) while allowing the local/dev demo stack (profile {@code dev}) to work
 * until the authentication workstream lands.
 *
 * <p>SEC-1 remains partially open: this class is a gate, not an authentication
 * mechanism. See self-Docs/DAY13_SECURITY_PERFORMANCE_FIXES.md.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Dev profile: the audit/demo stack runs unauthenticated today.
     * Route gating stays in {@link #securedFilterChain(HttpSecurity)}; this chain
     * keeps the OpenAPI/UI usable so the module can be demonstrated and measured.
     * Replace with real authentication before any non-dev deployment.
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        log.warn("SecurityConfig(dev): /api/** is open. No authentication infrastructure "
                + "exists yet; do not deploy the dev profile outside local development.");
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Default (non-dev, non-test) profile: routes are closed to anonymous callers.
     * No credentials provider is registered yet, so nothing can authenticate —
     * this is deliberately fail-closed until SEC-1 is completed.
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/**").hasAnyRole("OPERATOR", "SUPERVISOR", "QC_INSPECTOR")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
