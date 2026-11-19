package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    boolean existsByEmail(String email);

    Optional<Visitor> findByEmail(String email);

    /** The visitor a booking with this email belongs to (the oldest when the case differs). */
    Optional<Visitor> findFirstByEmailIgnoreCaseOrderByIdAsc(String email);

    /** The visitor without email a booking with this name belongs to. */
    Optional<Visitor> findFirstByFullNameIgnoreCaseAndEmailIsNullOrderByIdAsc(String fullName);
}
