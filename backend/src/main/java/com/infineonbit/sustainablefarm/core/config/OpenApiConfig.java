package com.infineonbit.sustainablefarm.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sustainableFarmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sustainable Farm API")
                        .version("1.0.0")
                        .description("Backend API for the sustainable farm: visitor management, "
                                + "registrations, scheduling, bookings and the educational program.")
                        .contact(new Contact().name("Sustainable Farm development team")));
    }
}