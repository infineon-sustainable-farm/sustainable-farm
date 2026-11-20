package com.infineonbit.sustainablefarm.modules.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.infineonbit.sustainablefarm.modules.machinery.api.Category;
import com.infineonbit.sustainablefarm.modules.machinery.api.Stage;
import com.infineonbit.sustainablefarm.modules.machinery.api.Status;
import com.infineonbit.sustainablefarm.modules.machinery.entity.Equipment;
import com.infineonbit.sustainablefarm.modules.machinery.repository.EquipmentRepository;
import com.infineonbit.sustainablefarm.modules.machinery.repository.OperatorAssignmentRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.staff.repository.PersonRepository;
import com.infineonbit.sustainablefarm.modules.visitormanagement.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AC-5 and the resource ownership rule: a person is entered once, on the visitor management
 * Staff screen (the people directory), and machinery and product transformation pick the same
 * {@code person} row by id; {@code GET /api/staff/persons} lists the active people.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SharedPersonApiTest {

    private static final String NAME = "Mariam Shared-Person";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private OperatorAssignmentRepository assignmentRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void machineryAndPtReferenceThePersonEnteredInTheDirectory() throws Exception {
        long staffId = createEntry(NAME, null);
        Long person = staffRepository.findById(staffId).orElseThrow().getPersonId();
        Equipment equipment = new Equipment(null, "Shared person tractor", Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION, Status.OPERATIONAL);
        equipment.setCode("EQ-SHARED-PERSON");
        equipment = equipmentRepository.save(equipment);
        long before = personRepository.count();

        mockMvc.perform(post("/api/machinery/operator-assignments").contentType(MediaType.APPLICATION_JSON).content("""
                {"personId":%d,"equipmentId":%d,"jobTitle":"Driver","startDate":"2026-01-01"}
                """.formatted(person, equipment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(NAME))
                .andExpect(jsonPath("$.personId").value(person))
                .andExpect(jsonPath("$.jobTitle").value("Driver"));
        mockMvc.perform(post("/api/product-transformation/operators").contentType(MediaType.APPLICATION_JSON).content("""
                {"operatorId":"OP-SHARED-PERSON","personId":%d,"role":"DRYER","hireDate":"2025-06-01"}
                """.formatted(person)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operatorName").value(NAME))
                .andExpect(jsonPath("$.activeStatus").value("ACTIVE"));

        assertThat(personRepository.count()).isEqualTo(before);
        assertThat(assignmentRepository.findAll()).filteredOn(a -> a.getEquipment().getId().equals(
                equipmentRepository.findByCode("EQ-SHARED-PERSON").orElseThrow().getId()))
                .extracting(a -> a.getPersonId()).containsExactly(person);
        assertThat(operatorRepository.findByOperatorId("OP-SHARED-PERSON").orElseThrow().getPersonId()).isEqualTo(person);
        mockMvc.perform(get("/api/staff/persons").param("search", "shared-person"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(person))
                .andExpect(jsonPath("$[0].fullName").value(NAME));
    }

    @Test
    void aConsumerCannotCreateAPersonFromAnUnknownId() throws Exception {
        long before = personRepository.count();

        mockMvc.perform(post("/api/product-transformation/operators").contentType(MediaType.APPLICATION_JSON).content("""
                {"operatorId":"OP-UNKNOWN-PERSON","personId":999999,"role":"DRYER","hireDate":"2025-06-01"}
                """))
                .andExpect(status().isBadRequest());

        assertThat(personRepository.count()).isEqualTo(before);
    }

    @Test
    void deactivatingAnEntryHidesThePersonAndLeavesItsPtOperatorRoleAlone() throws Exception {
        String name = "Rasmata Active-Flag";
        long staffId = createEntry(name, "GUIDE");
        Long person = staffRepository.findById(staffId).orElseThrow().getPersonId();
        createOperator("OP-ACTIVE-FLAG-1", person);

        mockMvc.perform(delete("/api/visitor-management/staff/" + staffId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/visitor-management/staff/" + staffId)).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/product-transformation/operators/OP-ACTIVE-FLAG-1")).andExpect(jsonPath("$.activeStatus").value("ACTIVE"));
        mockMvc.perform(get("/api/staff/persons").param("search", "Active-Flag"))
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(person)).isEmpty());
    }

    @Test
    void deactivatingAPtOperatorLeavesThePersonActive() throws Exception {
        long staffId = createEntry("Boukary Active-Flag", "GUIDE");
        Long person = staffRepository.findById(staffId).orElseThrow().getPersonId();
        createOperator("OP-ACTIVE-FLAG-2", person);

        mockMvc.perform(put("/api/product-transformation/operators/OP-ACTIVE-FLAG-2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activeStatus\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStatus").value("INACTIVE"));

        mockMvc.perform(get("/api/visitor-management/staff/" + staffId)).andExpect(jsonPath("$.active").value(true));
        assertThat(personRepository.findById(person).orElseThrow().isActive()).isTrue();
    }

    private void createOperator(String id, Long personId) throws Exception {
        mockMvc.perform(post("/api/product-transformation/operators").contentType(MediaType.APPLICATION_JSON).content("""
                {"operatorId":"%s","personId":%d,"role":"DRYER","hireDate":"2025-06-01"}
                """.formatted(id, personId)))
                .andExpect(status().isCreated());
    }

    /** An entry of the people directory, with a visitor role or none ({@code null}). */
    private long createEntry(String name, String role) throws Exception {
        String roleJson = role == null ? "null" : "\"" + role + "\"";
        String body = mockMvc.perform(post("/api/visitor-management/staff").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"%s","role":%s}
                """.formatted(name, roleJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
    }
}
