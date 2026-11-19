package com.infineonbit.sustainablefarm.modules.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
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
    void createTrimsAndStartsActive() {
        PersonDto person = directory.create(new PersonDetails("  Awa Traoré ", " awa@farm.local ", " ", null));

        assertThat(person.fullName()).isEqualTo("Awa Traoré");
        assertThat(person.email()).isEqualTo("awa@farm.local");
        assertThat(person.phone()).isNull();
        assertThat(person.active()).isTrue();
        assertThat(personRepository.count()).isEqualTo(1);
    }

    @Test
    void createNeverReusesAPersonByNameAndRefusesATakenEmail() {
        PersonDto first = directory.create(new PersonDetails("Moussa", "moussa@farm.local", null, null));
        PersonDto homonym = directory.create(new PersonDetails("Moussa", null, null, null));

        assertThat(homonym.id()).isNotEqualTo(first.id());
        assertThatThrownBy(() -> directory.create(new PersonDetails("Other", "MOUSSA@farm.local", null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void nameOfAndNamesOfReadTheFullName() {
        assertThat(directory.nameOf(null)).isNull();
        Long id = directory.create(new PersonDetails("Issa Ouédraogo", null, null, null)).id();
        assertThat(directory.nameOf(id)).isEqualTo("Issa Ouédraogo");
        assertThat(directory.namesOf(List.of(id, 999_999L))).containsOnlyKeys(id);
    }

    @Test
    void requireExistingRefusesAnUnknownIdAndKeepsNull() {
        Long id = directory.create(new PersonDetails("Known", null, null, null)).id();

        assertThat(directory.requireExisting(id)).isEqualTo(id);
        assertThat(directory.requireExisting(null)).isNull();
        assertThatThrownBy(() -> directory.requireExisting(999_999L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void searchActiveMatchesNameOrEmailAndSkipsInactivePeople() {
        directory.create(new PersonDetails("Alix Kaboré", "alix@farm.local", null, null));
        directory.create(new PersonDetails("Bintou", "bk@farm.local", null, null));
        directory.create(new PersonDetails("Alima", null, null, false));

        assertThat(directory.searchActive("ali")).extracting(PersonDto::fullName).containsExactly("Alix Kaboré");
        assertThat(directory.searchActive("BK@")).extracting(PersonDto::fullName).containsExactly("Bintou");
        assertThat(directory.searchActive(" ")).extracting(PersonDto::fullName).containsExactly("Alix Kaboré", "Bintou");
    }

    @Test
    void updateKeepsNullAttributesClearsBlankOnesAndRefusesATakenEmail() {
        PersonDto person = directory.create(new PersonDetails("Aida", "aida@farm.local", "123", null));
        directory.create(new PersonDetails("Other", "other@farm.local", null, null));

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
        assertThatThrownBy(() -> directory.create(new PersonDetails(" ", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
