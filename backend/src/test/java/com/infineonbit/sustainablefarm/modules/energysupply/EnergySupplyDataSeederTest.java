package com.infineonbit.sustainablefarm.modules.energysupply;

import com.infineonbit.sustainablefarm.core.config.SecurityConfig;
import com.infineonbit.sustainablefarm.modules.energysupply.controller.EnergyComponentController;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.EnergyComponent;
import com.infineonbit.sustainablefarm.modules.energysupply.exception.EnergySupplyExceptionHandler;
import com.infineonbit.sustainablefarm.modules.energysupply.repository.EnergyComponentRepository;
import com.infineonbit.sustainablefarm.modules.energysupply.service.EnergyComponentService;
import org.springframework.boot.SpringBootConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:energy-supply-dev-startup;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EnergySupplyDataSeederTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void devStartupSeedsEmptyDatabaseIsIdempotentAndCrudWorks() throws Exception {
        assertSeededDataCounts();

        String component = """
                {"componentId":"ENG-999","name":"Test battery","category":"BATTERY","capacityValue":3.5,"status":"OPERATIONAL"}
                """;
        mockMvc.perform(post("/api/energy/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(component))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.componentId").value("ENG-999"));
        mockMvc.perform(get("/api/energy/components/ENG-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test battery"));

        mockMvc.perform(put("/api/energy/components/ENG-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"ENG-999","name":"Updated battery","category":"BATTERY","capacityValue":4.5,"status":"OPERATIONAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated battery"));
        mockMvc.perform(delete("/api/energy/components/ENG-999"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/energy/components/ENG-999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/energy/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"bad","name":"","category":null,"capacityValue":-1,"status":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(post("/api/energy/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"ENG-001","name":"Duplicate","category":"PV_ARRAY","capacityValue":1,"status":"OPERATIONAL"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Energy component already exists: ENG-001"));
        mockMvc.perform(put("/api/energy/components/ENG-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"componentId":"ENG-998","name":"Mismatched ID","category":"BATTERY","capacityValue":1,"status":"OPERATIONAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Path componentId must match request componentId"));

        try (ConfigurableApplicationContext secondStartup = new SpringApplicationBuilder(EnergySupplyTestApplication.class)
                .profiles("dev")
                .run(
                        "--spring.datasource.url=jdbc:h2:mem:energy-supply-dev-startup;DB_CLOSE_DELAY=-1",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=none",
                        "--spring.flyway.enabled=false",
                        "--spring.main.web-application-type=none")) {
            assertEquals(5L, secondStartup.getBean(EnergyComponentRepository.class).count());
        }

        assertSeededDataCounts();
    }

    private void assertSeededDataCounts() {
        assertEquals(5, count("energy_component"));
        assertEquals(6, count("consumption_log"));
        assertEquals(1, count("carbon_metric"));
        assertEquals(1, count("generator_event"));
        assertEquals(7, count("solar_generation_log"));
        assertEquals(2, count("alert"));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = EnergyComponent.class)
    @EnableJpaRepositories(basePackageClasses = EnergyComponentRepository.class)
    @Import({
            EnergySupplyDataSeeder.class,
            SecurityConfig.class,
            EnergyComponentController.class,
            EnergyComponentService.class,
            EnergySupplyExceptionHandler.class
    })
    static class EnergySupplyTestApplication {
    }
}
