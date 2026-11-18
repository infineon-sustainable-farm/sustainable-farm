package com.infineonbit.sustainablefarm.modules.producttransformation.operator;

import static org.assertj.core.api.Assertions.assertThat;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator.Role;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.service.OperatorService;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** The product transformation operator is the role of a person of the staff referential (ADR 0004). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OperatorPersonIntegrationTest {

    @Autowired
    private OperatorService operatorService;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private PersonDirectory personDirectory;

    @Test
    void anOperatorReusesThePersonOfTheSameNameAndShowsItsNameAndStatus() {
        Long person = personDirectory.idOf("Salif PT-Person");

        Operator created = operatorService.createOperator(operator("OP-PERSON-01", " salif pt-person ", null));

        assertThat(created.getPersonId()).isEqualTo(person);
        assertThat(created.getOperatorName()).isEqualTo("Salif PT-Person");
        assertThat(created.getActiveStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(operatorRepository.findById("OP-PERSON-01").orElseThrow().getPersonId()).isEqualTo(person);
    }

    @Test
    void theActiveStatusIsThePersonsOne() {
        Operator created = operatorService.createOperator(operator("OP-PERSON-02", "Nafi PT-Person", ActiveStatus.ACTIVE));

        operatorService.updateActiveStatus("OP-PERSON-02", ActiveStatus.INACTIVE);

        assertThat(personDirectory.findById(created.getPersonId()).orElseThrow().active()).isFalse();
        assertThat(operatorService.isOperatorActive("OP-PERSON-02")).isFalse();
        assertThat(operatorService.getActiveOperatorsByRole(Role.DRYER))
                .extracting(Operator::getOperatorId).doesNotContain("OP-PERSON-02");
        assertThat(operatorService.getOperatorsByStatus(ActiveStatus.INACTIVE))
                .extracting(Operator::getOperatorId).contains("OP-PERSON-02");
    }

    @Test
    void twoOperatorRolesOfTheSameNameAreTwoPeople() {
        Operator first = operatorService.createOperator(operator("OP-PERSON-03", "Homonym PT-Person", null));
        Operator second = operatorService.createOperator(operator("OP-PERSON-04", "Homonym PT-Person", null));

        assertThat(second.getPersonId()).isNotEqualTo(first.getPersonId());
    }

    private static Operator operator(String id, String name, ActiveStatus status) {
        Operator operator = new Operator();
        operator.setOperatorId(id);
        operator.setOperatorName(name);
        operator.setRole(Role.DRYER);
        operator.setActiveStatus(status);
        operator.setHireDate(LocalDate.of(2025, 1, 1));
        return operator;
    }
}
