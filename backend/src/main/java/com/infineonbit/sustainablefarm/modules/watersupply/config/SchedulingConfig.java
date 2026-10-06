package com.infineonbit.sustainablefarm.modules.watersupply.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active la planification Spring dans le module.
 *
 * <p>Un seul besoin aujourd'hui : la verification periodique de l'humidite du sol qui declenche
 * une irrigation quand la culture en a besoin (voir IrrigationAutoTriggerJob). Le drapeau
 * {@code app.irrigation.auto-trigger-enabled} permet de la desactiver sans redeployer de code.</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
