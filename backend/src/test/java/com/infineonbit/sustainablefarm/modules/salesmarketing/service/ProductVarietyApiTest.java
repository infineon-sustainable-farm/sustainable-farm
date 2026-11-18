package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Product;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The variety of a product is a key of the plants catalogue, with a foreign key,
 * while the API still sends and receives a variety name.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ProductVarietyApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @BeforeEach
    void catalogue() {
        VarietyCatalogTestData.ensureDefaults(varietyReferenceRepository);
    }

    @Test
    void aVarietyNameIsResolvedToTheCatalogueAndSentBackByName() throws Exception {
        String body = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Mangue fraîche\", \"variety\": \"kent\", \"unit\": \"kg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variety").value("Kent"))
                .andExpect(jsonPath("$.varietyKey").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(body, "$.productId");

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].variety", hasItem("Kent")));

        mockMvc.perform(put("/api/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Mangue séchée\", \"variety\": \"Amélie\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variety").value("Amelie"));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(jsonPath("$.variety").value("Amelie"));
    }

    @Test
    void aProductMayHaveNoVariety() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Jus\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variety").doesNotExist());
    }

    @Test
    void anUnknownVarietyIsRefused() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Mangue\", \"variety\": \"Alphonso\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theDatabaseRefusesAKeyOutsideTheCatalogue() {
        Product product = new Product();
        product.setName("Mangue");
        product.setVarietyKey("ALPHONSO");

        assertThatThrownBy(() -> productRepository.saveAndFlush(product))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
