package com.infineonbit.sustainablefarm.modules.watersupply.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Thresholds and coefficients used by the inter-module integrations (Plants, Visitor Management,
 * Product Transformation, IoT commands).
 *
 * <p>Every value is overridable through an environment variable (see {@code application.properties}).
 * The defaults are working values, NOT validated business references: each one is listed as an open
 * question in {@code docs/integrations/} and must be confirmed by the owning team.</p>
 */
@Component
public class WaterIntegrationProperties {
    public final double qualityPhMin;
    public final double qualityPhMax;
    public final double qualityTurbidityMaxNtu;
    public final double rainDefaultCatchmentAreaM2;
    public final double rainDefaultRunoffCoefficient;
    public final int visitorLiveWindowMinutes;
    public final int safetyDefaultMaxCapacity;
    public final double recentRainSkipThresholdMm;
    public final int recentRainWindowHours;
    public final double ptWashingLitersPerKg;
    public final double ptCuttingLitersPerKg;
    public final double ptDryerCleaningM3PerLot;
    public final double ptBufferMinLiters;
    public final int valveMaxDurationSeconds;
    public final int iotOfflineThresholdMinutes;
    /** Multipliers applied to the FAO-56 need per Plants growth phase (1.0 = no adjustment). */
    public final double plantsEstablishmentFactor;
    public final double plantsGradualProductionFactor;
    public final double plantsFullProductionFactor;
    /** Days a quality test stays valid before a lot (provisional, to be agreed with Product Transformation). */
    public final int qualityTestValidityDays;

    public WaterIntegrationProperties(
            @Value("${app.water.quality.ph-min:6.0}") double qualityPhMin,
            @Value("${app.water.quality.ph-max:7.5}") double qualityPhMax,
            @Value("${app.water.quality.turbidity-max-ntu:5.0}") double qualityTurbidityMaxNtu,
            @Value("${app.water.rain.default-catchment-area-m2:180.0}") double rainDefaultCatchmentAreaM2,
            @Value("${app.water.rain.default-runoff-coefficient:0.8}") double rainDefaultRunoffCoefficient,
            @Value("${app.water.visitor.live-window-minutes:15}") int visitorLiveWindowMinutes,
            @Value("${app.water.visitor.safety-default-max-capacity:10}") int safetyDefaultMaxCapacity,
            @Value("${app.water.irrigation.recent-rain-skip-threshold-mm:5.0}") double recentRainSkipThresholdMm,
            @Value("${app.water.irrigation.recent-rain-window-hours:24}") int recentRainWindowHours,
            @Value("${app.water.pt.washing-liters-per-kg:2.0}") double ptWashingLitersPerKg,
            @Value("${app.water.pt.cutting-liters-per-kg:0.5}") double ptCuttingLitersPerKg,
            @Value("${app.water.pt.dryer-cleaning-m3-per-lot:1.0}") double ptDryerCleaningM3PerLot,
            @Value("${app.water.pt.buffer-min-liters:500.0}") double ptBufferMinLiters,
            @Value("${app.water.iot.valve-max-duration-seconds:30}") int valveMaxDurationSeconds,
            @Value("${app.water.iot.offline-threshold-minutes:15}") int iotOfflineThresholdMinutes,
            @Value("${app.water.plants.phase-factor.establishment:1.0}") double plantsEstablishmentFactor,
            @Value("${app.water.plants.phase-factor.gradual-production:1.0}") double plantsGradualProductionFactor,
            @Value("${app.water.plants.phase-factor.full-production:1.0}") double plantsFullProductionFactor,
            @Value("${app.water.quality.test-validity-days:7}") int qualityTestValidityDays) {
        this.qualityPhMin = qualityPhMin;
        this.qualityPhMax = qualityPhMax;
        this.qualityTurbidityMaxNtu = qualityTurbidityMaxNtu;
        this.rainDefaultCatchmentAreaM2 = rainDefaultCatchmentAreaM2;
        this.rainDefaultRunoffCoefficient = rainDefaultRunoffCoefficient;
        this.visitorLiveWindowMinutes = visitorLiveWindowMinutes;
        this.safetyDefaultMaxCapacity = safetyDefaultMaxCapacity;
        this.recentRainSkipThresholdMm = recentRainSkipThresholdMm;
        this.recentRainWindowHours = recentRainWindowHours;
        this.ptWashingLitersPerKg = ptWashingLitersPerKg;
        this.ptCuttingLitersPerKg = ptCuttingLitersPerKg;
        this.ptDryerCleaningM3PerLot = ptDryerCleaningM3PerLot;
        this.ptBufferMinLiters = ptBufferMinLiters;
        this.valveMaxDurationSeconds = valveMaxDurationSeconds;
        this.iotOfflineThresholdMinutes = iotOfflineThresholdMinutes;
        this.plantsEstablishmentFactor = plantsEstablishmentFactor;
        this.plantsGradualProductionFactor = plantsGradualProductionFactor;
        this.plantsFullProductionFactor = plantsFullProductionFactor;
        this.qualityTestValidityDays = qualityTestValidityDays;
    }

    /** Same values as the property defaults, for unit tests that do not start Spring. */
    public static WaterIntegrationProperties defaults() {
        return new WaterIntegrationProperties(6.0, 7.5, 5.0, 180.0, 0.8, 15, 10,
                5.0, 24, 2.0, 0.5, 1.0, 500.0, 30, 15, 1.0, 1.0, 1.0, 7);
    }
}
