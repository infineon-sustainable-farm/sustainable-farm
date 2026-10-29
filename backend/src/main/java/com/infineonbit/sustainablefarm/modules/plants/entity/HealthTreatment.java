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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A plant protection treatment of a block, one line of the treatment record
 * GLOBALG.A.P. asks for: block, date, applicator, the problem it targets as its
 * justification, the trade name and active ingredient of the product, the
 * pre-harvest interval, the quantity and the equipment.
 *
 * <p>A treatment answers a {@link HealthFinding}, or is preventive and has
 * none. When it answers a finding, the farm, the block, the targeted issue and
 * its label are copied from the finding; the copy is safe, since neither side
 * ever changes after it is written. {@code targetOtherLabel} names the problem
 * when the targeted issue is "Other", as for sulfur against powdery mildew, so
 * that every treatment can be recorded.
 *
 * <p>The date from which the block can be harvested is computed on every read
 * as {@code treatedOn} plus {@code preHarvestIntervalDays}, never stored. No
 * harvest is refused because of it. There is no stock of plant protection
 * products.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "health_treatment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HealthTreatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id")
    private Integer farmId;

    /** Block code as stored, trimmed and upper-cased, for example {@code "C"}. */
    @Column(name = "block_code", nullable = false)
    private String blockCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_issue_id", nullable = false)
    private HealthIssueReference targetIssue;

    @Column(name = "target_other_label", length = 60)
    private String targetOtherLabel;

    /** The finding the treatment answers, or {@code null} for a preventive treatment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id")
    private HealthFinding finding;

    @Column(name = "treated_on", nullable = false)
    private LocalDate treatedOn;

    /** Trade name of the product, for example {@code "Copper fungicide A"}. */
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "active_ingredient", nullable = false)
    private String activeIngredient;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 30)
    private TreatmentUnit unit;

    /** Days to wait after the treatment before harvesting, 0 or more. */
    @Column(name = "pre_harvest_interval_days", nullable = false)
    private Integer preHarvestIntervalDays;

    @Column(name = "applicator", nullable = false)
    private String applicator;

    @Column(name = "equipment")
    private String equipment;

    /** Where the row comes from, for example {@code "user_entry"} for the treatment routes. */
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
