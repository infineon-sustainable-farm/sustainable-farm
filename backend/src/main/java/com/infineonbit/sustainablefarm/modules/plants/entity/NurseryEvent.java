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
 * A dated fact about a {@link NurseryBatch}: a change of stage, a loss or a
 * transplant.
 *
 * <p>The event is stored, never the balance. The columns filled depend on the
 * type, and the service keeps to it:
 * <ul>
 *     <li>STAGE_CHANGE: {@code stage};</li>
 *     <li>LOSS: {@code quantity} and {@code reason};</li>
 *     <li>TRANSPLANT: {@code quantity}, {@code blockCode} and
 *         {@code populationEvent}, the PLANTING the transplant created.</li>
 * </ul>
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "nursery_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NurseryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private NurseryBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private NurseryEventType eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    /** The stage reached, for a stage change only. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30)
    private NurseryStage stage;

    /** Plants lost or transplanted, always positive; {@code null} for a stage change. */
    @Column(name = "quantity")
    private Integer quantity;

    /** Why the plants were lost, for a loss only. */
    @Column(name = "reason")
    private String reason;

    /** Block the plants went to, stored like the block of a planting, for a transplant only. */
    @Column(name = "block_code")
    private String blockCode;

    /** The PLANTING event the transplant created in the orchard, for a transplant only. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "population_event_id")
    private PopulationEvent populationEvent;

    /** Where the row comes from, for example {@code "user_entry"} for the nursery routes. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
