package com.infineonbit.sustainablefarm.modules.watersupply.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring scheduling in the module.
 *
 * <p>One need today: the periodic soil moisture check that triggers an irrigation when the
 * crop needs it (see IrrigationAutoTriggerJob). The flag
 * {@code app.irrigation.auto-trigger-enabled} allows disabling it without redeploying code.</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
