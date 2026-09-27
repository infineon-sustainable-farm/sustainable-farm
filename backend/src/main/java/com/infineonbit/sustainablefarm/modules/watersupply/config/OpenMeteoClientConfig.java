package com.infineonbit.sustainablefarm.modules.watersupply.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the Open-Meteo weather API.
 *
 * <p>Named for the service it calls rather than "WebConfig": the MVC/CORS
 * configuration lives in {@code core.config.WebConfig}, and two beans cannot
 * share that name.
 */
@Configuration
public class OpenMeteoClientConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
    }
}
