package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.plants.service.GrowthCalendarService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process adapter on the Plants growth calendar ({@code GET /api/plants/growth-calendar}).
 *
 * <p>The application is a modular monolith: modules call each other in the same JVM instead of
 * over HTTP. Only the public service and its {@code GrowthCalendarResponse}, documented by Plants as
 * "the contract other modules consume", are used. Plants identifies farms with an {@code Integer}
 * while Water Supply uses UUIDs, so the lookup is by block code only (open question for Plants).</p>
 *
 * <p>A Plants failure never breaks irrigation: the adapter answers "unknown" and logs.</p>
 */
@Component
public class PlantsGrowthCalendarAdapter implements PlantsGrowthPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlantsGrowthCalendarAdapter.class);

    private final GrowthCalendarService growthCalendarService;

    public PlantsGrowthCalendarAdapter(GrowthCalendarService growthCalendarService) {
        this.growthCalendarService = growthCalendarService;
    }

    @Override
    public List<BlockGrowth> growthForBlock(String blockCode) {
        try {
            return growthCalendarService.getAllGrowthCalendarEntries(null, blockCode).stream()
                    .map(entry -> new BlockGrowth(entry.blockCode(), entry.growthPhase(), entry.ageYears()))
                    .toList();
        } catch (RuntimeException ex) {
            LOGGER.warn("Plants growth calendar unavailable for block {}", blockCode, ex);
            return List.of();
        }
    }
}
