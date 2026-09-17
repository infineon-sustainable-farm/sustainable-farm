package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Planting calendar of a block of the farm.
 *
 * <p>Column names are the domain names of the schema and are mapped explicitly,
 * like {@link Variete}.
 *
 * <p>There is deliberately no tree-age column. The age of the trees is never
 * stored nor entered: it is computed from {@code datePlantation} and the
 * current date when the entry is read (see the service layer).
 *
 * <p>{@code stadeActuel} is a field observation, not the computed growth phase.
 * It stays NULL until someone records it.
 */
@Entity
@Table(name = "calendrier_croissance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendrierCroissance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_ferme")
    private Integer idFerme;

    @Column(name = "bloc_parcelle", nullable = false)
    private String blocParcelle;

    /** Not provided by the source study. Stays NULL until recorded. */
    @Column(name = "date_plantation")
    private LocalDate datePlantation;

    /** How precise the planting date is, in the words the source allows. */
    @Column(name = "precision_date")
    private String precisionDate;

    /** Observed growth stage. Not the computed phase. Stays NULL until observed. */
    @Column(name = "stade_actuel")
    private String stadeActuel;

    @Column(name = "phase_annees")
    private String phaseAnnees;

    /** Local rainfall for this block. The source only gives a farm-wide range. */
    @Column(name = "pluviometrie_locale_mm")
    private Double pluviometrieLocaleMm;

    @Column(name = "source")
    private String source;

    @Column(name = "date_maj")
    private Instant dateMaj;
}
