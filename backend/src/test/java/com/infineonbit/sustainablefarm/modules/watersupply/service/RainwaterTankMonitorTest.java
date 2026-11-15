package com.infineonbit.sustainablefarm.modules.watersupply.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Rainwater harvesting tank rules (module 5.3): likely overflow, use of
 * rainwater first, and silence on sources that are not rainwater.
 */
@ExtendWith(MockitoExtension.class)
class RainwaterTankMonitorTest {

    @Mock
    private AlertService alertService;

    @Test
    void classifiesLevelsAgainstTheDocumentedThresholds() {
        assertEquals("unknown", RainwaterTankRules.classify(null));
        assertEquals("critical", RainwaterTankRules.classify(10d));
        assertEquals("moderate", RainwaterTankRules.classify(35d));
        assertEquals("comfortable", RainwaterTankRules.classify(60d));
        assertEquals("full", RainwaterTankRules.classify(96d));
    }

    @Test
    void raisesAnOverflowRiskAlertAboveNinetyFivePercent() {
        when(alertService.raiseOnce(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new Notification()));

        List<String> raised = monitor(0d).evaluate(tank(1000d, 960d));

        assertEquals(List.of("overflow_risk"), raised);
        verify(alertService).raiseOnce(eq("critical"), contains("overflow risk"), anyString(),
                eq("/watersupply/rainwater"));
    }

    @Test
    void recommendsRainwaterFirstOnlyWhenTheSiteDeclaresANonIrrigationNeed() {
        // Without a declared non-irrigation need, a half-full tank triggers nothing.
        assertTrue(monitor(0d).evaluate(tank(1000d, 700d)).isEmpty());
        verifyNoInteractions(alertService);
    }

    @Test
    void recommendsRainwaterFirstAboveFiftyPercentWithANeed() {
        when(alertService.raiseOnce(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new Notification()));

        List<String> raised = monitor(500d).evaluate(tank(1000d, 700d));

        assertEquals(List.of("use_rainwater_first"), raised);
        verify(alertService).raiseOnce(eq("warning"), contains("Use rainwater first"), anyString(),
                eq("/watersupply/rainwater"));
    }

    @Test
    void ignoresSourcesThatAreNotRainwaterTanks() {
        WaterSource borehole = tank(1000d, 990d);
        borehole.setType("borehole");

        assertTrue(monitor(500d).evaluate(borehole).isEmpty());
        verifyNoInteractions(alertService);
    }

    @Test
    void doesNotCountAnAlertThatIsAlreadyOpen() {
        when(alertService.raiseOnce(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertTrue(monitor(0d).evaluate(tank(1000d, 960d)).isEmpty());
    }

    private WaterSource tank(double capacityLiters, double currentLevelLiters) {
        WaterSource source = new WaterSource();
        source.setName("Rain tank");
        source.setType(RainwaterTankRules.RAINWATER_SOURCE_TYPE);
        source.setCapacityLiters(capacityLiters);
        source.setCurrentLevelLiters(currentLevelLiters);
        return source;
    }

    private RainwaterTankMonitor monitor(double weeklyNonIrrigationNeedLiters) {
        return new RainwaterTankMonitor(alertService, weeklyNonIrrigationNeedLiters);
    }
}
