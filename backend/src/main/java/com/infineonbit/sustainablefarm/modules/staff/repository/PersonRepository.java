package com.infineonbit.sustainablefarm.modules.staff.repository;

import com.infineonbit.sustainablefarm.modules.staff.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findAllByFullNameIgnoreCaseOrderByIdAsc(String fullName);

    Optional<Person> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    List<Person> findAllByActiveTrueOrderByFullNameAscIdAsc();
}
