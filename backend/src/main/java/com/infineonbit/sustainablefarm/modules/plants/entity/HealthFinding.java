package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A problem seen during a {@link HealthInspection}: an issue of the catalogue,
 * optionally on one tree.
 *
 * <p>The row holds no status. The status is computed on every read from the
 * treatments of the finding and its resolution. {@code resolvedOn} and
 * {@code resolutionNote} are written once, by the resolution route, and never
 * change afterwards: the resolution is a dated event, not a status.
 *
 * <p>{@code otherLabel} names the problem when the issue is "Other", and only
 * then. The farm and the block are those of the inspection.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "health_finding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private HealthInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private HealthIssueReference issue;

    @Column(name = "other_label", length = 60)
    private String otherLabel;

    /** The tree concerned, for example {@code "42"}, or {@code null} for the block as a whole. */
    @Column(name = "tree_label", length = 20)
    private String treeLabel;

    @Column(name = "resolved_on")
    private LocalDate resolvedOn;

    @Column(name = "resolution_note")
    private String resolutionNote;

    /** Where the row comes from, for example {@code "user_entry"} for the inspection route. */
    @Column(name = "source", nullable = false)
    private String source;

    /** Write time of the finding, then of its resolution, its only change. */
    @Column(name = "last_updated")
    private Instant lastUpdated;
}
