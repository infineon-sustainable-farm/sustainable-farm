package com.infineonbit.sustainablefarm.modules.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

        mockMvc.perform(post("/api/operator-assignments").contentType(MediaType.APPLICATION_JSON).content("""
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
        mockMvc.perform(post("/api/v1/staff").contentType(MediaType.APPLICATION_JSON).content("""
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
}
