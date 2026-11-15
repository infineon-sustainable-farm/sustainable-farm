package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentityProvider;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves module authorization from a central user id. Global platform roles
 * are deliberately not used as Farm Security roles.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSecurityAuthorizationService {

    private final SiteSecurityIdentityProvider identityProvider;
    private final OperatorRepository operatorRepository;

    public Optional<SiteSecurityPrincipal> currentPrincipal() {
        return identityProvider.current().flatMap(this::resolve);
    }

    private Optional<SiteSecurityPrincipal> resolve(SiteSecurityIdentity identity) {
        if (identity.development()) {
            return Optional.of(new SiteSecurityPrincipal(identity.centralUserId(),
                    SiteSecurityRole.ADMIN, java.util.Set.of(), java.util.Set.of()));
        }
        return operatorRepository.findByCentralUserId(identity.centralUserId())
                .filter(OperatorEntity::isAuthorizationActive)
                .map(operator -> operator.toPrincipal(identity));
    }
}
