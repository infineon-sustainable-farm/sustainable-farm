package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.DevelopmentSiteSecurityIdentityProvider;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityIdentityProvider;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityOperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityOperatorRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The module role, permissions and zones come from the operator assignment of the caller's person. */
@ExtendWith(MockitoExtension.class)
class SiteSecurityAuthorizationServiceTest {

    @Mock
    private SiteSecurityIdentityProvider identityProvider;

    @Mock
    private SiteSecurityOperatorRepository operatorRepository;

    @InjectMocks
    private SiteSecurityAuthorizationService service;

    @Test
    void noIdentity_noPrincipal() {
        when(identityProvider.current()).thenReturn(Optional.empty());

        assertFalse(service.currentPrincipal().isPresent());
        verifyNoInteractions(operatorRepository);
    }

    @Test
    void aPersonWithoutAssignment_getsNothing() {
        when(identityProvider.current()).thenReturn(Optional.of(new SiteSecurityIdentity(42L, Set.of("ADMIN"), false)));
        when(operatorRepository.findByPersonId(42L)).thenReturn(Optional.empty());

        assertFalse(service.currentPrincipal().isPresent());
    }

    @Test
    void aRegularUser_getsItsPermissionsAndZones() {
        SiteSecurityOperatorEntity operator = SiteSecurityOperatorEntity.builder()
                .id("op-1").personId(42L).role(SiteSecurityRole.USER)
                .permissions(Set.of(Permission.VIEW_ZONES.name())).zoneIds(Set.of("open")).build();
        when(identityProvider.current()).thenReturn(Optional.of(new SiteSecurityIdentity(42L, Set.of(), false)));
        when(operatorRepository.findByPersonId(42L)).thenReturn(Optional.of(operator));

        SiteSecurityPrincipal principal = service.currentPrincipal().orElseThrow();

        assertEquals("op-1", principal.operatorId());
        assertEquals(SiteSecurityRole.USER, principal.role());
        assertTrue(principal.can(Permission.VIEW_ZONES));
        assertFalse(principal.can(Permission.VIEW_ACCESS_LOG));
        assertTrue(principal.seesZone("open"));
        assertFalse(principal.seesZone("critical"));
    }

    @Test
    void aGateRoleAssignment_isNotAnActiveAuthorization() {
        SiteSecurityOperatorEntity operator = SiteSecurityOperatorEntity.builder()
                .id("op-2").personId(43L).role(SiteSecurityRole.GATE).build();
        when(identityProvider.current()).thenReturn(Optional.of(new SiteSecurityIdentity(43L, Set.of(), false)));
        when(operatorRepository.findByPersonId(43L)).thenReturn(Optional.of(operator));

        assertFalse(service.currentPrincipal().isPresent());
    }

    @Test
    void theDevelopmentIdentity_actsAsAdmin_withoutReadingAssignments() {
        when(identityProvider.current()).thenReturn(new DevelopmentSiteSecurityIdentityProvider().current());

        SiteSecurityPrincipal principal = service.currentPrincipal().orElseThrow();

        assertTrue(principal.isAdmin());
        assertEquals(DevelopmentSiteSecurityIdentityProvider.DEVELOPMENT_ACTOR, principal.actor());
        verifyNoInteractions(operatorRepository);
    }
}
