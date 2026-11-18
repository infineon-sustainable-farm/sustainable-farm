package com.infineonbit.sustainablefarm.modules.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDetails;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import com.infineonbit.sustainablefarm.modules.staff.repository.PersonRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(PersonDirectoryService.class)
class PersonDirectoryServiceTest {

    @Autowired
    private PersonDirectoryService directory;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void theSameNameGivesTheSamePerson() {
        PersonDto first = directory.findOrCreateByName("  Awa Traoré ");
        PersonDto second = directory.findOrCreateByName("awa traoré");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(first.fullName()).isEqualTo("Awa Traoré");
        assertThat(first.active()).isTrue();
        assertThat(personRepository.count()).isEqualTo(1);
    }

    @Test
    void idOfABlankNameIsNullAndNameOfAnIdIsTheFullName() {
        assertThat(directory.idOf("  ")).isNull();
        assertThat(directory.nameOf(null)).isNull();
        Long id = directory.idOf("Issa Ouédraogo");
        assertThat(directory.nameOf(id)).isEqualTo("Issa Ouédraogo");
        assertThat(directory.namesOf(List.of(id, 999_999L))).containsOnlyKeys(id);
    }

    @Test
    void anEmailNamesOnePersonWhateverTheName() {
        PersonDto guide = directory.findOrCreate(new PersonDetails("Alix", "alix@farm.local", null, null), id -> true);
        PersonDto again = directory.findOrCreate(new PersonDetails("Alix Kaboré", "ALIX@farm.local", null, null), id -> true);

        assertThat(again.id()).isEqualTo(guide.id());
    }

    @Test
    void aNameMatchReceivesTheMissingEmailButNotAnotherOne() {
        PersonDto byName = directory.findOrCreateByName("Moussa");
        PersonDto withEmail = directory.findOrCreate(new PersonDetails("Moussa", "moussa@farm.local", "+22670000000", null), id -> true);
        PersonDto otherMoussa = directory.findOrCreate(new PersonDetails("Moussa", "other@farm.local", null, null), id -> true);

        assertThat(withEmail.id()).isEqualTo(byName.id());
        assertThat(withEmail.email()).isEqualTo("moussa@farm.local");
        assertThat(withEmail.phone()).isEqualTo("+22670000000");
        assertThat(otherMoussa.id()).isNotEqualTo(byName.id());
    }

    @Test
    void anIneligiblePersonIsNotReusedByName() {
        PersonDto first = directory.findOrCreateByName("Guide");
        PersonDto second = directory.findOrCreate(PersonDetails.named("Guide"), id -> id != first.id());

        assertThat(second.id()).isNotEqualTo(first.id());
    }

    @Test
    void updateKeepsNullAttributesClearsBlankOnesAndRefusesATakenEmail() {
        PersonDto person = directory.findOrCreate(new PersonDetails("Aida", "aida@farm.local", "123", null), id -> true);
        directory.findOrCreate(new PersonDetails("Other", "other@farm.local", null, null), id -> true);

        PersonDto renamed = directory.update(person.id(), new PersonDetails("Aida Sawadogo", null, "", false));
        assertThat(renamed.fullName()).isEqualTo("Aida Sawadogo");
        assertThat(renamed.email()).isEqualTo("aida@farm.local");
        assertThat(renamed.phone()).isNull();
        assertThat(renamed.active()).isFalse();
        assertThat(directory.listActive()).extracting(PersonDto::fullName).containsExactly("Other");

        assertThatThrownBy(() -> directory.update(person.id(), new PersonDetails(null, "other@farm.local", null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void aPersonNeedsAName() {
        assertThatThrownBy(() -> directory.findOrCreateByName(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
