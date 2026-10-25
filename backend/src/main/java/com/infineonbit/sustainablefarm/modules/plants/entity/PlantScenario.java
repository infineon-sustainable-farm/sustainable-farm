package com.infineonbit.sustainablefarm.modules.plants.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A planting plan to analyse: how many trees, what one tree yields, what the
 * mangoes sell for and what the plan costs, as the user enters them.
 *
 * <p>A scenario is a projection, attached to no farm and no block: nothing in it
 * is planted. The orchard that is planted lives in the variety rows and their
 * population events, and the yield forecast never reads this table. Only the
 * assumptions are stored, each with its source; every result (revenue, net
 * benefit, return on investment, payback) is computed on every read by
 * {@code ScenarioAnalysisService}.
 *
 * <p>The optional values stay {@code null} until the user enters them, never 0,
 * and their source is {@code null} with them. All amounts are in
 * {@link #currency}.
 *
 * <p>The table and its columns follow the naming of the other modules: English
 * snake_case and a singular table name.
 */
@Entity
@Table(name = "plant_scenario",
        uniqueConstraints = @UniqueConstraint(name = "uk_plant_scenario_name", columnNames = "name"),
        check = {
                @CheckConstraint(
                        name = "ck_plant_scenario_tree_count",
                        constraint = "tree_count >= 1"),
                @CheckConstraint(
                        name = "ck_plant_scenario_loss_rate",
                        constraint = "loss_rate BETWEEN 0 AND 1")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Name of the scenario, unique ignoring case, accents and surrounding spaces. */
    @Column(name = "name", nullable = false)
    private String name;

    /** Variety the plan is about, for information only; {@code null} when not given. */
    @Column(name = "variety_name")
    private String varietyName;

    /** Number of trees of the plan, all planted in its first year. */
    @Column(name = "tree_count", nullable = false)
    private Integer treeCount;

    @Column(name = "tree_count_source", nullable = false)
    private String treeCountSource;

    /** Yield of one tree in full production, per year, in kg. */
    @Column(name = "yield_per_tree_kg", nullable = false)
    private Double yieldPerTreeKg;

    @Column(name = "yield_source", nullable = false)
    private String yieldSource;

    /** Share of the harvest that is lost before sale, from 0 to 1; {@code null} until entered. */
    @Column(name = "loss_rate")
    private Double lossRate;

    @Column(name = "loss_rate_source")
    private String lossRateSource;

    /** Selling price of one kg of fresh mangoes; {@code null} until entered. */
    @Column(name = "price_per_kg", precision = 14, scale = 4)
    private BigDecimal pricePerKg;

    @Column(name = "price_source")
    private String priceSource;

    /**
     * ISO 4217 code of every amount of the scenario. Plain text without a CHECK
     * constraint, like {@link FertilizerMovement#getCurrency()}: the codes are
     * checked by the API.
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Operating cost of a year whatever the harvest, possibly 0. */
    @Column(name = "annual_fixed_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal annualFixedCost;

    @Column(name = "annual_fixed_cost_source", nullable = false)
    private String annualFixedCostSource;

    /** Operating cost of each kg sold, such as packaging or transport; {@code null} until entered. */
    @Column(name = "variable_cost_per_kg", precision = 14, scale = 4)
    private BigDecimal variableCostPerKg;

    @Column(name = "variable_cost_source")
    private String variableCostSource;

    /** Total investment of the plan, spent before its first year. */
    @Column(name = "initial_investment", nullable = false, precision = 14, scale = 2)
    private BigDecimal initialInvestment;

    @Column(name = "investment_source", nullable = false)
    private String investmentSource;

    /** Investment for each tree added by a variant, plant and planting; {@code null} until entered. */
    @Column(name = "cost_per_additional_tree", precision = 14, scale = 2)
    private BigDecimal costPerAdditionalTree;

    @Column(name = "cost_per_additional_tree_source")
    private String costPerAdditionalTreeSource;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
