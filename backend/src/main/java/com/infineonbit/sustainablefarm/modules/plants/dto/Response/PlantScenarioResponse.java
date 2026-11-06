package com.infineonbit.sustainablefarm.modules.plants.dto.Response;

import java.time.Instant;

/**
 * API representation of a planting scenario: its assumptions as entered, each
 * with its source. No result is stored, so none is given here: see
 * {@link ScenarioAnalysisResponse}.
 *
 * <p>An optional value not entered is {@code null}, and so is its source.
 *
 * @param id                          identifier of the scenario
 * @param name                        name of the scenario
 * @param varietyName                 variety the plan is about, possibly {@code null}
 * @param treeCount                   number of trees of the plan
 * @param treeCountSource             source of the number of trees
 * @param yieldPerTreeKg              yearly yield of one tree in full production, in kg
 * @param yieldSource                 source of the yield
 * @param lossRate                    share of the harvest lost before sale, from 0 to 1
 * @param lossRateSource              source of the loss rate
 * @param pricePerKg                  selling price of one kg, in {@code currency}
 * @param priceSource                 source of the price
 * @param currency                    ISO 4217 code of every amount, XOF or EUR
 * @param annualFixedCost             operating cost of a year whatever the harvest
 * @param annualFixedCostSource       source of the fixed cost
 * @param variableCostPerKg           operating cost of each kg sold
 * @param variableCostSource          source of the variable cost
 * @param initialInvestment           total investment of the plan
 * @param investmentSource            source of the investment
 * @param costPerAdditionalTree       investment for each tree added by a variant
 * @param costPerAdditionalTreeSource source of that investment
 * @param lastUpdated                 when the scenario was entered or last corrected
 */
public record PlantScenarioResponse(
        Long id,
        String name,
        String varietyName,
        Integer treeCount,
        String treeCountSource,
        Double yieldPerTreeKg,
        String yieldSource,
        Double lossRate,
        String lossRateSource,
        Double pricePerKg,
        String priceSource,
        String currency,
        Double annualFixedCost,
        String annualFixedCostSource,
        Double variableCostPerKg,
        String variableCostSource,
        Double initialInvestment,
        String investmentSource,
        Double costPerAdditionalTree,
        String costPerAdditionalTreeSource,
        Instant lastUpdated) {
}
