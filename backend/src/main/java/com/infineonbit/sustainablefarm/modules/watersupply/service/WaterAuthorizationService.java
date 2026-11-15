package com.infineonbit.sustainablefarm.modules.watersupply.service;

import org.springframework.stereotype.Service;

/**
 * Authorization hook for actions with physical effect (valve commands).
 *
 * <p>Authentication and roles belong to the global platform, which does not exist yet: this hook
 * lets every request through for now. It is the single place to plug the platform's check (for
 * example a role "water_operator") without touching the command logic.</p>
 */
@Service
public class WaterAuthorizationService {
    public void checkPhysicalCommandAllowed(String action) {
        // To be wired to the platform authorization once it exists (see docs/integrations).
    }
}
