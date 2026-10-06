package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Surveillance du reservoir alimente par la collecte de pluie (module 5.3 de la specification).
 *
 * <p>Deux situations sont signalees :</p>
 * <ul>
 *   <li>reservoir presque plein ({@value #OVERFLOW_RISK_PERCENT} % et plus) : le volume capte
 *       risque de deborder, donc d'etre perdu ;</li>
 *   <li>reservoir a moitie plein ({@value #USE_RAINWATER_FIRST_PERCENT} % et plus) alors que le
 *       site declare un besoin en eau non-irrigation (lavage, transformation) : il faut puiser
 *       dans l'eau de pluie, gratuite, avant l'eau pompee qui coute de l'energie.</li>
 * </ul>
 *
 * <p>Les seuils sont evalues a chaque mesure de niveau envoyee par le capteur du reservoir
 * (voir IotTelemetryService) : c'est le seul moment ou la grandeur change.</p>
 */
@Service
public class RainwaterTankMonitor {

    /** Niveau a partir duquel un debordement est probable (voir RainwaterTankRules). */
    public static final double OVERFLOW_RISK_PERCENT = RainwaterTankRules.OVERFLOW_RISK_PERCENT;

    /** Niveau a partir duquel l'eau de pluie doit etre utilisee en priorite (voir RainwaterTankRules). */
    public static final double USE_RAINWATER_FIRST_PERCENT = RainwaterTankRules.USE_RAINWATER_FIRST_PERCENT;

    private final AlertService alertService;
    private final double weeklyNonIrrigationNeedLiters;

    public RainwaterTankMonitor(
            AlertService alertService,
            @Value("${app.rainwater.weekly-nonirrigation-need-liters:0}") double weeklyNonIrrigationNeedLiters) {
        this.alertService = alertService;
        this.weeklyNonIrrigationNeedLiters = weeklyNonIrrigationNeedLiters;
    }

    /** Vrai si la source est un reservoir alimente par la collecte de pluie. */
    public boolean isRainwaterTank(WaterSource source) {
        return RainwaterTankRules.isRainwaterTank(source);
    }

    /** Niveau du reservoir en pourcentage de sa capacite, ou null si la capacite est inconnue. */
    public Double levelPercent(WaterSource source) {
        return RainwaterTankRules.levelPercent(source);
    }

    /**
     * Evalue les regles du reservoir de pluie et leve les alertes necessaires.
     *
     * @return les codes des alertes effectivement levees ({@code overflow_risk},
     *         {@code use_rainwater_first}) ; liste vide si tout est normal ou si l'alerte
     *         existe deja (deduplication de {@link AlertService#raiseOnce}).
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
