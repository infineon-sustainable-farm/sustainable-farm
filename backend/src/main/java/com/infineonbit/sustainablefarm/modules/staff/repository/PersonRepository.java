package com.infineonbit.sustainablefarm.modules.staff.repository;

import com.infineonbit.sustainablefarm.modules.staff.entity.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findAllByFullNameIgnoreCaseOrderByIdAsc(String fullName);

    Optional<Person> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    List<Person> findAllByActiveTrueOrderByFullNameAscIdAsc();

    /** Active people whose full name or email contains the term, ignoring case. */
    @Query("select p from Person p where p.active = true and (lower(p.fullName) like lower(concat('%', :term, '%'))"
            + " or lower(coalesce(p.email, '')) like lower(concat('%', :term, '%'))) order by p.fullName asc, p.id asc")
    List<Person> searchActive(@Param("term") String term);
}
