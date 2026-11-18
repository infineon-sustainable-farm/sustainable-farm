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
 * AC-5: a person entered once is the same {@code person} row for machinery, product
 * transformation and visitor management, through their unchanged APIs.
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
    void machineryPtAndVisitorManagementReferenceTheSamePerson() throws Exception {
        Equipment equipment = new Equipment(null, "Shared person tractor", Category.AGRICULTURAL_MACHINERY,
                Stage.CULTIVATION, Status.OPERATIONAL);
        equipment.setCode("EQ-SHARED-PERSON");
        equipment = equipmentRepository.save(equipment);

        mockMvc.perform(post("/api/machinery/operator-assignments").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"%s","equipmentId":%d,"jobTitle":"Driver","startDate":"2026-01-01"}
                """.formatted(NAME, equipment.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(NAME))
                .andExpect(jsonPath("$.jobTitle").value("Driver"));
        mockMvc.perform(post("/api/operators").contentType(MediaType.APPLICATION_JSON).content("""
                {"operatorId":"OP-SHARED-PERSON","operatorName":"%s","role":"DRYER","hireDate":"2025-06-01"}
                """.formatted(NAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operatorName").value(NAME))
                .andExpect(jsonPath("$.activeStatus").value("ACTIVE"));
        mockMvc.perform(post("/api/visitor-management/staff").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"%s","role":"GUIDE"}
                """.formatted(NAME)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(NAME));

        assertThat(personRepository.findAllByFullNameIgnoreCaseOrderByIdAsc(NAME)).hasSize(1);
        Long person = personRepository.findAllByFullNameIgnoreCaseOrderByIdAsc(NAME).get(0).getId();
        assertThat(assignmentRepository.findAll()).filteredOn(a -> a.getEquipment().getId().equals(
                equipmentRepository.findByCode("EQ-SHARED-PERSON").orElseThrow().getId()))
                .extracting(a -> a.getPersonId()).containsExactly(person);
        assertThat(operatorRepository.findById("OP-SHARED-PERSON").orElseThrow().getPersonId()).isEqualTo(person);
        assertThat(staffRepository.findAll()).filteredOn(s -> person.equals(s.getPersonId())).hasSize(1);
    }

    @Test
    void deactivatingAGuideLeavesTheSamePersonsPtOperatorActive() throws Exception {
        String name = "Rasmata Active-Flag";
        createOperator("OP-ACTIVE-FLAG-1", name);
        long staffId = createGuide(name);

        mockMvc.perform(delete("/api/visitor-management/staff/" + staffId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/visitor-management/staff/" + staffId)).andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(get("/api/operators/OP-ACTIVE-FLAG-1")).andExpect(jsonPath("$.activeStatus").value("ACTIVE"));
        assertThat(personRepository.findAllByFullNameIgnoreCaseOrderByIdAsc(name)).singleElement()
                .satisfies(person -> assertThat(person.isActive()).isTrue());
    }

    @Test
    void deactivatingAPtOperatorLeavesTheSamePersonsGuideActive() throws Exception {
        String name = "Boukary Active-Flag";
        createOperator("OP-ACTIVE-FLAG-2", name);
        long staffId = createGuide(name);

        mockMvc.perform(put("/api/operators/OP-ACTIVE-FLAG-2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activeStatus\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStatus").value("INACTIVE"));

        mockMvc.perform(get("/api/visitor-management/staff/" + staffId)).andExpect(jsonPath("$.active").value(true));
        assertThat(personRepository.findAllByFullNameIgnoreCaseOrderByIdAsc(name)).singleElement()
                .satisfies(person -> assertThat(person.isActive()).isTrue());
    }

    private void createOperator(String id, String name) throws Exception {
        mockMvc.perform(post("/api/operators").contentType(MediaType.APPLICATION_JSON).content("""
                {"operatorId":"%s","operatorName":"%s","role":"DRYER","hireDate":"2025-06-01"}
                """.formatted(id, name)))
                .andExpect(status().isCreated());
    }

    private long createGuide(String name) throws Exception {
        String body = mockMvc.perform(post("/api/visitor-management/staff").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName":"%s","role":"GUIDE"}
                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
    }
}
