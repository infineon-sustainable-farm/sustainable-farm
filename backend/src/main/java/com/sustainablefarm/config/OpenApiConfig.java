package com.sustainablefarm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Product Transformation API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productTransformationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sustainable Farm Product Transformation API")
                        .description("""
                                REST API for mango processing traceability, quality control, \
                                compliance, and export readiness workflows.

                                Business errors are returned as structured JSON with fields: \
                                timestamp, status, error, message, path, and optional validationErrors.""")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Abdoul Ben Fatao SANON")
                                .email("abdoul.sanon@example.com"))
                        .license(new License().name("BIT × Infineon Excellence Program")))
                .tags(List.of(
                        new Tag().name("Batch Management").description("Core batch lifecycle and status transitions"),
                        new Tag().name("Raw Intake Management").description("Raw material intake from Plants"),
                        new Tag().name("Wash & Sort Management").description("Washing and sorting operations"),
                        new Tag().name("Drying Management").description("Drying process records"),
                        new Tag().name("Packaging Management").description("Packaging and export readiness"),
                        new Tag().name("Quality Control").description("QC checkpoints and mandatory inspections"),
                        new Tag().name("Compliance Management").description("HACCP compliance audits"),
                        new Tag().name("Equipment Management").description("Equipment registry and maintenance"),
                        new Tag().name("Operator Management").description("Personnel and role management"),
                        new Tag().name("Harvest Integration").description("Harvest events from Plants workstream"),
                        new Tag().name("Historical Harvest").description("Aggregated data for forecasting")
                ));
    }
}
