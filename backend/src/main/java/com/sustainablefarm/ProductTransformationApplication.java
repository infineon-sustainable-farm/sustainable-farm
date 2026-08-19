package com.sustainablefarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application for Product Transformation System
 * 
 * This application implements the validated MERISE MCD from Week 5 Phase 3,
 * providing batch traceability for sustainable dried mango production.
 * 
 * Technology Stack: Spring Boot 3.2.0 + PostgreSQL + React
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@SpringBootApplication
public class ProductTransformationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductTransformationApplication.class, args);
        System.out.println("Product Transformation System started successfully!");
        System.out.println("API Documentation: http://localhost:8080/swagger-ui.html");
    }
}