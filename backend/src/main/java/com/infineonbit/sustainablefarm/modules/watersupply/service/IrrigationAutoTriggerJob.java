package com.infineonbit.sustainablefarm.modules.watersupply.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic soil moisture check (specification module 1.4).
 *
 * <p>The job only calls {@link IrrigationAutomationService#trigger()}: the rule stays in the
 * same place as the manual endpoint {@code POST /api/irrigations/auto-trigger}, so testing
 * the API also tests the job.</p>
 *
 * <p>The check can be disabled without redeployment with
 * {@code app.irrigation.auto-trigger-enabled=false}, and the period is configurable through
 * {@code app.irrigation.auto-trigger-interval-ms} (15 minutes by default).</p>
 */
@Component
public class IrrigationAutoTriggerJob {

    private static final Logger log = LoggerFactory.getLogger(IrrigationAutoTriggerJob.class);

    private final IrrigationAutomationService automationService;
    private final boolean enabled;

    public IrrigationAutoTriggerJob(
            IrrigationAutomationService automationService,
            @Value("${app.irrigation.auto-trigger-enabled:true}") boolean enabled) {
        this.automationService = automationService;
        this.enabled = enabled;
    }

    /** A job failure must never propagate: it is logged and retried on the next round. */
    @Scheduled(
            initialDelayString = "${app.irrigation.auto-trigger-initial-delay-ms:60000}",
            fixedDelayString = "${app.irrigation.auto-trigger-interval-ms:900000}")
    public void checkSoilMoisture() {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> report = automationService.trigger();
            log.info("Automatic irrigation check: {} schedule(s) created, {} zone(s) skipped",
                    report.get("created_count"), report.get("skipped_count"));
        } catch (RuntimeException ex) {
            log.warn("Automatic irrigation check failed: {}", ex.getMessage());
        }
    }
}
