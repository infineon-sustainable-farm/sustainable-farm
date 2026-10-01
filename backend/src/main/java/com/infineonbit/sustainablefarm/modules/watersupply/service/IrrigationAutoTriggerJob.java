package com.infineonbit.sustainablefarm.modules.watersupply.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Verification periodique de l'humidite du sol (module 1.4 de la specification).
 *
 * <p>La tache ne fait qu'appeler {@link IrrigationAutomationService#trigger()} : la regle reste
 * au meme endroit que l'endpoint manuel {@code POST /api/irrigations/auto-trigger}, donc tester
 * l'API teste aussi la tache.</p>
 *
 * <p>Le controle peut etre desactive sans redeploiement avec
 * {@code app.irrigation.auto-trigger-enabled=false}, et la periode est reglable par
 * {@code app.irrigation.auto-trigger-interval-ms} (15 minutes par defaut).</p>
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

    /** Un echec de la tache ne doit jamais remonter : il est journalise et retentera au tour suivant. */
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
