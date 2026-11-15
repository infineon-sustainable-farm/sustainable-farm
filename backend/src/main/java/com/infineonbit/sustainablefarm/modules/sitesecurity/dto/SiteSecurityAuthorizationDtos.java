package com.infineonbit.sustainablefarm.modules.sitesecurity.dto;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import java.util.List;

public final class SiteSecurityAuthorizationDtos {

    private SiteSecurityAuthorizationDtos() {
    }

    public record AuthorizationDto(String centralUserId, String role, List<String> permissions,
            List<String> zoneIds) {
        public static AuthorizationDto of(SiteSecurityPrincipal principal) {
            return new AuthorizationDto(principal.operatorId(), principal.role().name().toLowerCase(),
                    principal.permissions().stream().sorted().toList(), principal.zoneIds().stream().sorted().toList());
        }
    }
}
