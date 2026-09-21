package com.sustainablefarm.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for external module integrations
 * 
 * Provides RestTemplate bean for calling external module APIs
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Configuration
public class IntegrationConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
