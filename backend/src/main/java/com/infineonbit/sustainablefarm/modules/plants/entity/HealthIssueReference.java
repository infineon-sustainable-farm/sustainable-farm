package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A pest or disease of the mango orchard that an inspection can report, such as
 * "Anthracnose" or "Fruit flies".
 *
 * <p>A reference row, sourced and editable in the database, loaded by
 * {@code HealthIssueReferenceLoader}. The catalogue is a table and not a Java
 * enum: an enum stored as text gets a CHECK constraint that
 * {@code ddl-auto=update} never widens, so a new issue would need a migration.
 * Here a new issue is one more row, and the code column has no CHECK.
 *
 * <p>{@code code} is the stable key the API receives, upper case, such as
 * {@code ANTHRACNOSE}. {@code eppoCode} is the EPPO code of the organism when it
 * has one, the reference of plant protection services.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "health_issue_reference",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_health_issue_reference_code",
                columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthIssueReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 30)
    private HealthIssueKind kind;

    @Column(name = "scientific_name")
    private String scientificName;

    @Column(name = "eppo_code", length = 10)
    private String eppoCode;

    /** Where the row comes from, as a code such as {@code "Dianda_2025"}. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
