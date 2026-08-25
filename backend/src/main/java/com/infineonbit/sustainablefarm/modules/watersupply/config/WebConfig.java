package com.infineonbit.sustainablefarm.modules.watersupply.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
    }
}
