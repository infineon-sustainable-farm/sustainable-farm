package com.infineonbit.sustainablefarm.modules.salesmarketing;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Sales and Marketing API on the normalised model: Long ids under the former field names,
 * lower-case statuses, euro amounts as numbers, foreign keys between the rows and the column
 * defaults. Not transactional, so that the foreign keys are checked on commit as in the
 * application; every name is unique to the run.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SalesMarketingApiTest {

    private static final String API = "/api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final String run = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void anOrderKeepsTheJsonOfTheFormerEntity() throws Exception {
        long customer = customer();
        long channel = create("/sales-channels", "{\"name\":\"Export " + run + "\"}", "channelId");

        String body = mockMvc.perform(post(API + "/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + customer + ",\"salesChannelId\":" + channel
                                + ",\"orderDate\":\"2026-02-10\",\"totalVolumeKg\":1200.5,\"totalValueEur\":8400,"
                                + "\"status\":\"pending\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer))
                .andExpect(jsonPath("$.salesChannelId").value(channel))
                .andExpect(jsonPath("$.orderDate").value("2026-02-10"))
                .andExpect(jsonPath("$.totalValueEur").value(8400.0))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.customer").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long order = ((Number) JsonPath.read(body, "$.orderId")).longValue();

        mockMvc.perform(get(API + "/orders/{id}", order))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValueEur").value(8400.0))
                .andExpect(jsonPath("$.totalVolumeKg").value(1200.5));
        assertThat(jdbc.queryForObject("select total_value_currency from orders where id = ?", String.class, order))
                .isEqualTo("EUR");
    }

    @Test
    void statusesKeepTheirLowerCaseValuesAndAnUnknownOneIsAConflict() throws Exception {
        mockMvc.perform(post(API + "/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme\",\"customerCode\":\"D" + run + "\","
                                + "\"customerType\":\"direct_export\",\"status\":\"prospect\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerType").value("direct_export"))
                .andExpect(jsonPath("$.status").value("prospect"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(post(API + "/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Bad\",\"customerType\":\"wholesaler\",\"status\":\"active\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(API + "/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"A\",\"destination\":\"B\",\"status\":\"lost\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get(API + "/customers").param("status", "nonsense"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingValuesTakeTheDefaultsOfTheirColumns() throws Exception {
        mockMvc.perform(post(API + "/campaigns").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Minimal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("scheduled"))
                .andExpect(jsonPath("$.budgetEur").doesNotExist());
        mockMvc.perform(post(API + "/shipments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"A\",\"destination\":\"B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("packed"));
        mockMvc.perform(post(API + "/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Juice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.unit").value("kg"))
                .andExpect(jsonPath("$.createdAt").exists());
        mockMvc.perform(post(API + "/product-batches").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchCode\":\"M" + run + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncedAt").exists());
    }

    @Test
    void anUnknownReferencedRowIsRefusedByItsForeignKey() throws Exception {
        mockMvc.perform(post(API + "/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":987654321,\"orderDate\":\"2026-02-10\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aCustomerWithOrdersIsKeptAndItsContactsAndCertificationsGoWithIt() throws Exception {
        long customer = customer();
        long contact = create("/customer-contacts",
                "{\"customerId\":" + customer + ",\"fullName\":\"Anna\"}", "contactId");
        long certification = create("/certifications", "{\"name\":\"Organic " + run + "\"}", "certificationId");
        mockMvc.perform(post(API + "/customers/{c}/certifications/{k}", customer, certification))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer))
                .andExpect(jsonPath("$.certificationId").value(certification));
        mockMvc.perform(post(API + "/customers/{c}/certifications/{k}", customer, certification))
                .andExpect(status().isCreated());
        mockMvc.perform(get(API + "/customers/{c}/certifications", customer))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(API + "/customer-contacts/{id}", contact))
                .andExpect(jsonPath("$.isPrimary").value(false))
                .andExpect(jsonPath("$.customerId").value(customer));

        long order = create("/orders",
                "{\"customerId\":" + customer + ",\"orderDate\":\"2026-02-10\"}", "orderId");
        mockMvc.perform(delete(API + "/customers/{id}", customer)).andExpect(status().isConflict());

        mockMvc.perform(delete(API + "/orders/{id}", order)).andExpect(status().isNoContent());
        mockMvc.perform(delete(API + "/customers/{id}", customer)).andExpect(status().isNoContent());
        mockMvc.perform(get(API + "/customer-contacts/{id}", contact)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from customer_certifications where customer_id = ?",
                Integer.class, customer)).isZero();
    }

    @Test
    void anUpdateReplacesTheFieldsAndKeepsTheId() throws Exception {
        long customer = customer();
        mockMvc.perform(put(API + "/customers/{id}", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme GmbH\",\"customerType\":\"retailer\",\"status\":\"inactive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer))
                .andExpect(jsonPath("$.companyName").value("Acme GmbH"))
                .andExpect(jsonPath("$.customerCode").doesNotExist())
                .andExpect(jsonPath("$.status").value("inactive"));
        mockMvc.perform(get(API + "/customers").param("status", "inactive"))
                .andExpect(jsonPath("$[?(@.customerId == " + customer + ")]", hasSize(1)));
    }

    private long customer() throws Exception {
        return create("/customers", "{\"companyName\":\"Acme\",\"customerCode\":\"C" + run + System.nanoTime() % 100000
                + "\",\"customerType\":\"distributor\",\"status\":\"active\"}", "customerId");
    }

    private long create(String path, String json, String idField) throws Exception {
        String body = mockMvc.perform(post(API + path).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$." + idField)).longValue();
    }
}
