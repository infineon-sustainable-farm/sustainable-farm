package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A dated change in the number of trees of a variety row.
 *
 * <p>The event is stored, never the balance. The current number of trees of a
 * variety is computed from its events on every read (see
 * {@link com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository}),
 * the same way the tree age is computed from the planting date.
 *
 * <p>The event holds no farm and no block: a {@link Variety} row already stands
 * for one (farm, block, variety) triple, and copying them here would make two
 * sources of truth.
 *
 * <p>Unlike {@code varietes} and {@code calendrier_croissance}, the table and its
 * columns follow the naming of the other modules: English snake_case and a
 * singular table name.
 */
@Entity
@Table(name = "population_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopulationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variety_id", nullable = false)
    private Variety variety;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private PopulationEventType eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /** Number of trees concerned. Always positive: the event type says whether they are added or removed. */
    @Column(name = "tree_count", nullable = false)
    private Integer treeCount;

    /** Where the event comes from, for example {@code "user_entry"} for the planting form. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
