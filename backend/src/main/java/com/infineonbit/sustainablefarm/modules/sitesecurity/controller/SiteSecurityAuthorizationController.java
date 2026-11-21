package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AnyAccount;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityAccessInterceptor;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityAuthorizationDtos.AuthorizationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the current module authorization context for the operational UI.
 * Assignment management remains owned by the central platform integration.
 */
@RestController
@RequestMapping("/api/site-security/authorization")
@RequiredArgsConstructor
public class SiteSecurityAuthorizationController {

    @GetMapping("/current")
    @AnyAccount
    public AuthorizationDto current(
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal principal) {
        return AuthorizationDto.of(principal);
    }
}
