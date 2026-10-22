package com.infineonbit.sustainablefarm.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate Configuration
 * Provides RestTemplate bean for HTTP-based inter-module integration
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
