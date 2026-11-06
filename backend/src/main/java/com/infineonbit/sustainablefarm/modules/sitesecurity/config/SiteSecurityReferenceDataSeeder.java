package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zones are reference data, needed in every environment: a credential cannot
 * be created without them. Existing zones are never overwritten.
 */
@Component
@RequiredArgsConstructor
public class SiteSecurityReferenceDataSeeder implements CommandLineRunner {

    private final ZoneRepository zoneRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (zoneRepository.count() > 0) {
            return;
        }
        zoneRepository.saveAll(Arrays.asList(
            ZoneEntity.builder().id("open").name("Open Zone").tagline("Visitor-facing areas").status("Open Access").description("Public access areas including farm shop, main drive, and visitor center.").requirements(Arrays.asList("No clearance needed", "Visitor check-in recommended")).build(),
            ZoneEntity.builder().id("controlled").name("Controlled Zone").tagline("Operational farm grounds").status("Monitored").description("Active farming zones including barns, storage sheds, and equipment yards.").requirements(Arrays.asList("Staff or escorted visitor pass", "Safety briefing completed")).build(),
            ZoneEntity.builder().id("restricted").name("Restricted Zone").tagline("Processing & chemical storage").status("Protected").description("High-value equipment, product transformation, and hazardous chemical stores.").requirements(Arrays.asList("Level 3 badge clearance", "Dual-custody access protocol")).build(),
            ZoneEntity.builder().id("critical").name("Critical Zone").tagline("Server, power & water control").status("Locked Down").description("Infrastructure backbone: primary power distribution, main water valves, and security control room.").requirements(Arrays.asList("Level 4 clearance + biometric", "Security escort required", "Continuous audit logging")).build()
        ));
    }
}
