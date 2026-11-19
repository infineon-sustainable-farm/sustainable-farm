package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.VisitorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class VisitorRepositoryTest {

    @Autowired
    private VisitorRepository repository;

    private Visitor createVisitor(String name, String email) {
        Visitor v = new Visitor();
        v.setFullName(name);
        v.setGroupSize(1);
        v.setEmail(email);
        v.setType(VisitorType.INDIVIDUAL);
        return repository.save(v);
    }

    @Test
    void existsByEmail_true() {
        createVisitor("Alice", "alice@test.com");
        assertThat(repository.existsByEmail("alice@test.com")).isTrue();
    }

    @Test
    void existsByEmail_false() {
        assertThat(repository.existsByEmail("nobody@test.com")).isFalse();
    }

    @Test
    void findByEmail_present() {
        Visitor saved = createVisitor("Bob", "bob@test.com");
        assertThat(repository.findByEmail("bob@test.com")).isPresent();
        assertThat(repository.findByEmail("bob@test.com").get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByEmail_absent() {
        assertThat(repository.findByEmail("missing@test.com")).isEmpty();
    }
}
