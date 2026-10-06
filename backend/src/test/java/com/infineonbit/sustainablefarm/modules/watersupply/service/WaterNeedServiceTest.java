package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Reference agronomique du module (FAO-56) : besoin = surface x ET0 x Kc / efficacite du systeme.
 * C'est la formule qui sert de base au KPI « eau economisee » : elle doit rester verrouillee.
 */
class WaterNeedServiceTest {

    private final WaterNeedService service = new WaterNeedService();

    @Test
    void needIsAreaTimesEt0TimesKcDividedByEfficiency() {
        Zone zone = zone(1.0, "drip", 1.0);

        // 1 ha = 10 000 m2 ; 10 000 x 5 mm x 1.0 / 0.90
        assertEquals(55_555.56d, service.needLiters(zone, 5.0), 0.01d);
    }

    @Test
    void cropCoefficientIsAppliedWhenSet() {
        Zone zone = zone(2.0, "sprinkler", 0.8);

        // 20 000 x 5 x 0.8 / 0.75
        assertEquals(106_666.67d, service.needLiters(zone, 5.0), 0.01d);
    }

    @Test
    void efficiencyDependsOnTheIrrigationMethod() {
        assertEquals(0.90d, service.systemEfficiency(zone(1.0, "drip", 1.0)));
        assertEquals(0.90d, service.systemEfficiency(zone(1.0, "Goutte-a-goutte", 1.0)));
        assertEquals(0.75d, service.systemEfficiency(zone(1.0, "sprinkler", 1.0)));
        assertEquals(0.75d, service.systemEfficiency(zone(1.0, "aspersion", 1.0)));
        assertEquals(0.60d, service.systemEfficiency(zone(1.0, "gravity", 1.0)));
        assertEquals(0.80d, service.systemEfficiency(zone(1.0, "unknown-method", 1.0)));
        assertEquals(0.80d, service.systemEfficiency(zone(1.0, null, 1.0)));
    }

    @Test
    void missingOrInvalidDataFallsBackInsteadOfBreaking() {
        assertEquals(1.0d, service.cropCoefficient(zone(1.0, "drip", null)));
        assertEquals(1.0d, service.cropCoefficient(zone(1.0, "drip", 0d)));
        assertEquals(0d, service.needLiters(null, 5.0));
        assertEquals(0d, service.needLiters(zone(null, "drip", 1.0), 5.0));
        assertEquals(0d, service.needLiters(zone(1.0, "drip", 1.0), 0d));
    }

    @Test
    void cumulativeNeedSummsTheDailyNeeds() {
        Zone zone = zone(1.0, "drip", 1.0);

        // Une journee sans ET0 (null) ne compte pas : 2 jours de besoin au lieu de 3.
        // Arrays.asList est utilise car List.of refuse les elements null.
        assertEquals(111_111.12d, service.needLiters(zone, java.util.Arrays.asList(5.0, null, 5.0)), 0.01d);
    }

    @Test
    void methodLabelIsStableForTheInterface() {
        assertEquals("drip", service.methodLabel(zone(1.0, "goutte", 1.0)));
        assertEquals("sprinkler", service.methodLabel(zone(1.0, "Aspersion", 1.0)));
        assertEquals("gravity", service.methodLabel(zone(1.0, "flood", 1.0)));
        assertEquals("not specified", service.methodLabel(zone(1.0, null, 1.0)));
    }

    private Zone zone(Double areaHectares, String irrigationMethod, Double cropCoefficient) {
        Zone zone = new Zone();
        zone.setName("Zone A");
        zone.setAreaHectares(areaHectares);
        zone.setIrrigationMethod(irrigationMethod);
        zone.setCropCoefficient(cropCoefficient);
        return zone;
    }
}
