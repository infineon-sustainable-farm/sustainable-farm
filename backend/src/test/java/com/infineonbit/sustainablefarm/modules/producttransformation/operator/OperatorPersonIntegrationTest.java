package com.infineonbit.sustainablefarm.modules.producttransformation.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Operator;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.ActiveStatus;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.model.Role;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.operator.service.OperatorService;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.TestPeople;
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
    void anOperatorIsTheRoleOfAnExistingPersonAndShowsItsName() {
        Long person = TestPeople.idOf(personDirectory, "Salif PT-Person");

        Operator created = operatorService.createOperator(operator("OP-PERSON-01", person, null));

        assertThat(created.getPersonId()).isEqualTo(person);
        assertThat(created.getOperatorName()).isEqualTo("Salif PT-Person");
        assertThat(created.getActiveStatus()).isEqualTo(ActiveStatus.ACTIVE);
        assertThat(operatorRepository.findByOperatorId("OP-PERSON-01").orElseThrow().getPersonId()).isEqualTo(person);
    }

    @Test
    void theActiveStatusIsTheOperatorsOwnAndLeavesThePersonAlone() {
        Operator created = operatorService.createOperator(
                operator("OP-PERSON-02", TestPeople.idOf(personDirectory, "Nafi PT-Person"), ActiveStatus.ACTIVE));

        operatorService.updateActiveStatus("OP-PERSON-02", ActiveStatus.INACTIVE);

        assertThat(personDirectory.findById(created.getPersonId()).orElseThrow().active()).isTrue();
        assertThat(operatorService.isOperatorActive("OP-PERSON-02")).isFalse();
        assertThat(operatorService.getActiveOperatorsByRole(Role.DRYER))
                .extracting(Operator::getOperatorId).doesNotContain("OP-PERSON-02");
        assertThat(operatorService.getOperatorsByStatus(ActiveStatus.INACTIVE))
                .extracting(Operator::getOperatorId).contains("OP-PERSON-02");
    }

    @Test
    void anUnknownPersonIsRefusedAndNoPersonIsCreated() {
        int before = personDirectory.listActive().size();

        assertThatThrownBy(() -> operatorService.createOperator(operator("OP-PERSON-03", 999_999L, null)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(personDirectory.listActive()).hasSize(before);
        assertThat(operatorRepository.findByOperatorId("OP-PERSON-03")).isEmpty();
    }

    @Test
    void aPersonHoldsOneOperatorRoleAtMost() {
        Long person = TestPeople.idOf(personDirectory, "Homonym PT-Person");
        operatorService.createOperator(operator("OP-PERSON-04", person, null));

        assertThatThrownBy(() -> operatorService.createOperator(operator("OP-PERSON-05", person, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updatingTheRoleNeverRenamesThePerson() {
        Long person = TestPeople.idOf(personDirectory, "Rename PT-Person");
        operatorService.createOperator(operator("OP-PERSON-06", person, null));
        Operator change = operator("OP-PERSON-06", person, null);
        change.setOperatorName("Another Name");
        change.setRole(Role.WASHER);

        Operator updated = operatorService.updateOperator("OP-PERSON-06", change);

        assertThat(updated.getRole()).isEqualTo(Role.WASHER);
        assertThat(updated.getOperatorName()).isEqualTo("Rename PT-Person");
        assertThat(personDirectory.nameOf(person)).isEqualTo("Rename PT-Person");
    }

    private static Operator operator(String id, Long personId, ActiveStatus status) {
        Operator operator = new Operator();
        operator.setOperatorId(id);
        operator.setPersonId(personId);
        operator.setRole(Role.DRYER);
        operator.setActiveStatus(status);
        operator.setHireDate(LocalDate.of(2025, 1, 1));
        return operator;
    }
}
