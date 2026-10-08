package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monitoring of the rainwater harvesting reservoir (specification module 5.3).
 *
 * <p>Two situations are reported:</p>
 * <ul>
 *   <li>reservoir almost full ({@value #OVERFLOW_RISK_PERCENT} % and above): the captured volume
 *       risks overflowing, hence being lost;</li>
 *   <li>reservoir half full ({@value #USE_RAINWATER_FIRST_PERCENT} % and above) while the
 *       site declares a non-irrigation water need (washing, processing): the free rainwater
 *       must be used before the pumped water that costs energy.</li>
 * </ul>
 *
 * <p>The thresholds are evaluated at every level measurement sent by the reservoir sensor
 * (see IotTelemetryService): that is the only moment the quantity changes.</p>
 */
@Service
public class RainwaterTankMonitor {

    /** Level from which an overflow is likely (see RainwaterTankRules). */
    public static final double OVERFLOW_RISK_PERCENT = RainwaterTankRules.OVERFLOW_RISK_PERCENT;

    /** Level from which rainwater must be used first (see RainwaterTankRules). */
    public static final double USE_RAINWATER_FIRST_PERCENT = RainwaterTankRules.USE_RAINWATER_FIRST_PERCENT;

    private final AlertService alertService;
    private final double weeklyNonIrrigationNeedLiters;

    public RainwaterTankMonitor(
            AlertService alertService,
            @Value("${app.rainwater.weekly-nonirrigation-need-liters:0}") double weeklyNonIrrigationNeedLiters) {
        this.alertService = alertService;
        this.weeklyNonIrrigationNeedLiters = weeklyNonIrrigationNeedLiters;
    }

    /** True when the source is a reservoir fed by rainwater harvesting. */
    public boolean isRainwaterTank(WaterSource source) {
        return RainwaterTankRules.isRainwaterTank(source);
    }

    /** Tank level as a percentage of its capacity, or null when the capacity is unknown. */
    public Double levelPercent(WaterSource source) {
        return RainwaterTankRules.levelPercent(source);
    }

    /**
     * Evaluates the rainwater tank rules and raises the necessary alerts.
     *
     * @return the codes of the alerts actually raised ({@code overflow_risk},
     *         {@code use_rainwater_first}); empty list when everything is normal or when the alert
     *         already exists (deduplication of {@link AlertService#raiseOnce}).
     */
    @Transactional
    public List<String> evaluate(WaterSource source) {
        List<String> raised = new ArrayList<>();
        if (!isRainwaterTank(source)) {
            return raised;
        }
        Double percent = levelPercent(source);
        if (percent == null) {
            return raised;
        }
        String name = source.getName() == null ? "rainwater tank" : source.getName();

        if (percent >= OVERFLOW_RISK_PERCENT) {
            alertService.raiseOnce("critical", "Rainwater tank overflow risk - " + name,
                    String.format(
                            "The rainwater tank \"%s\" is %.0f%% full (%.0f L of %.0f L): harvested water "
                                    + "may overflow and be lost. Irrigate or transfer the surplus.",
                            name, percent, source.getCurrentLevelLiters(), source.getCapacityLiters()),
                    "/watersupply/rainwater").ifPresent(notification -> raised.add("overflow_risk"));
        }

        if (percent >= USE_RAINWATER_FIRST_PERCENT && weeklyNonIrrigationNeedLiters > 0) {
            alertService.raiseOnce("warning", "Use rainwater first - " + name,
                    String.format(
                            "The rainwater tank \"%s\" is %.0f%% full while the site declares %.0f L of "
                                    + "non-irrigation need this week: use this free water before pumping.",
                            name, percent, weeklyNonIrrigationNeedLiters),
                    "/watersupply/rainwater").ifPresent(notification -> raised.add("use_rainwater_first"));
        }

        return raised;
    }
}
