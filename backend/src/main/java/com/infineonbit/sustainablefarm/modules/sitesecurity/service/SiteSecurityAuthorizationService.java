package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.DevelopmentSiteSecurityIdentityProvider;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentityProvider;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityOperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityOperatorRepository;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the Site Security authorization of the caller's person: its operator assignment
 * (role, permissions, zones). A person without an active assignment gets nothing. Platform
 * roles are deliberately not used as Site Security roles.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSecurityAuthorizationService {

    private final SiteSecurityIdentityProvider identityProvider;
    private final SiteSecurityOperatorRepository operatorRepository;

    public Optional<SiteSecurityPrincipal> currentPrincipal() {
        return identityProvider.current().flatMap(this::resolve);
    }

    Optional<SiteSecurityPrincipal> resolve(SiteSecurityIdentity identity) {
        if (identity.development()) {
            return Optional.of(new SiteSecurityPrincipal(DevelopmentSiteSecurityIdentityProvider.DEVELOPMENT_ACTOR,
                    SiteSecurityRole.ADMIN, Set.of(), Set.of()));
        }
        return operatorRepository.findByPersonId(identity.personId())
                .filter(SiteSecurityOperatorEntity::isAuthorizationActive)
                .map(SiteSecurityOperatorEntity::toPrincipal);
    }
}
