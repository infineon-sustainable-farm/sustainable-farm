package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AnyAccount;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityAccessInterceptor;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.UserPermission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.*;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.Permission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LogFilter;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SiteSecurityService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Farm Security data. Reads are open to users holding the matching permission
 * (data restricted to their zones); everything else needs the admin role.
 */
@RestController
@RequestMapping("/api/v1/sitesecurity")
@RequiredArgsConstructor
public class SiteSecurityController {

    private final SiteSecurityService siteSecurityService;
    @AnyAccount
    @GetMapping("/session")
    public ResponseEntity<SessionDto> getSession(
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal principal) {
        return ResponseEntity.ok(SessionDto.of(principal));
    }

    @UserPermission(Permission.VIEW_DASHBOARD)
    @GetMapping("/overview")
    public ResponseEntity<OverviewResponseDto> getOverview(
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal principal) {
        return ResponseEntity.ok(siteSecurityService.getOverview(principal));
    }

    @UserPermission(Permission.VIEW_ZONES)
    @GetMapping("/zones")
    public ResponseEntity<List<ZoneDto>> getZones(
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal principal) {
        return ResponseEntity.ok(siteSecurityService.getZones(principal));
    }

    @UserPermission(Permission.VIEW_ACCESS_REGISTER)
    @GetMapping("/credentials")
    public ResponseEntity<List<UserDto>> getCredentials(
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal principal) {
        return ResponseEntity.ok(siteSecurityService.getCredentials(principal));
    }

    @PostMapping("/credentials")
    public ResponseEntity<UserDto> createCredential(@Valid @RequestBody CreateUserRequestDto request,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        UserDto created = siteSecurityService.createCredential(request, caller);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/credentials/{id}/revoke")
    public ResponseEntity<UserDto> revokeCredential(@PathVariable String id,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return ResponseEntity.ok(siteSecurityService.revokeCredential(id, caller));
    }

    @UserPermission(Permission.VIEW_ACCESS_LOG)
    @GetMapping("/logs")
    public ResponseEntity<List<LogEntryDto>> getAccessLogs(
            @RequestParam(required = false, defaultValue = "all") LogFilter filter,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal principal) {
        return ResponseEntity.ok(siteSecurityService.getAccessLogs(filter, principal));
    }
}
