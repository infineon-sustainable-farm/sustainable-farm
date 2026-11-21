package com.infineonbit.sustainablefarm.modules.sitesecurity.dto;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import java.util.List;
import java.util.Locale;

public final class SiteSecurityAuthorizationDtos {

    private SiteSecurityAuthorizationDtos() {
    }

    public record AuthorizationDto(String operatorId, String role, List<String> permissions,
            List<String> zoneIds) {
        public static AuthorizationDto of(SiteSecurityPrincipal principal) {
            return new AuthorizationDto(principal.operatorId(), principal.role().name().toLowerCase(Locale.ROOT),
                    principal.isAdmin() ? List.of()
                            : principal.permissions().stream().map(p -> p.toLowerCase(Locale.ROOT)).sorted().toList(),
                    principal.zoneIds().stream().sorted().toList());
        }
    }
}
