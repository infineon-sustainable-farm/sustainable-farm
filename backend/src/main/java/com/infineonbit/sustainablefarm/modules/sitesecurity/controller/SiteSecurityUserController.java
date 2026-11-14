package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AdminOnly;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityAccessInterceptor;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.CodeSignInStatusDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.ConfirmDeliveryRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.CopiedLinkDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.EmailStateDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.HistoryEntryDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.InviteUserRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.RetireRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UpdateUserRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UserDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.UserSummaryDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.CodeSignInSettings;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** Admin management of Farm Security accounts and invitations. Every endpoint requires ADMIN. */
@RestController
@RequestMapping("/api/v1/sitesecurity")
@RequiredArgsConstructor
@AdminOnly
public class SiteSecurityUserController {

    private final AccountService accounts;
    private final CodeSignInSettings codeSignIn;

    @GetMapping("/users")
    public List<UserDto> list() {
        return accounts.list();
    }

    @GetMapping("/users/summary")
    public UserSummaryDto summary() {
        return accounts.summary();
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> invite(@Valid @RequestBody InviteUserRequestDto request, @RequestAttribute(
            SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.invite(request, caller));
    }

    /** PUT and PATCH both replace the editable fields (the shared frontend client has no PUT). */
    @RequestMapping(path = "/users/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public UserDto update(@PathVariable String id, @Valid @RequestBody UpdateUserRequestDto request,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return accounts.update(id, request, caller);
    }

    @PostMapping("/users/{id}/invitation")
    public UserDto resend(@PathVariable String id,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return accounts.resendInvitation(id, caller);
    }

    @PostMapping("/users/{id}/invitation/revoke")
    public UserDto revoke(@PathVariable String id,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return accounts.revokeInvitation(id, caller);
    }

    @PostMapping("/users/{id}/invitation/link")
    public CopiedLinkDto copyLink(@PathVariable String id,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return accounts.copyInvitationLink(id, caller);
    }

    @PostMapping("/users/{id}/disable")
    public UserDto disable(@PathVariable String id,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return accounts.disable(id, caller);
    }

    @PostMapping("/users/{id}/enable")
    public UserDto enable(@PathVariable String id,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return accounts.enable(id, caller);
    }

    @GetMapping("/users/{id}/history")
    public List<HistoryEntryDto> history(@PathVariable String id) {
        return accounts.history(id);
    }

    /** Every email sent about the account (invitations, setup and reset links, notices), newest first. */
    @GetMapping("/users/{id}/emails")
    public List<EmailStateDto> emails(@PathVariable String id) {
        return accounts.emails(id);
    }

    @GetMapping("/settings/code-sign-in")
    public CodeSignInStatusDto codeSignInStatus() {
        return codeSignIn.status();
    }

    @PostMapping("/settings/code-sign-in/retire")
    public CodeSignInStatusDto retire(@Valid @RequestBody RetireRequestDto request,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return codeSignIn.retire(caller, request.getConfirmation());
    }

    @PostMapping("/settings/code-sign-in/restore")
    public CodeSignInStatusDto restore(
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        return codeSignIn.restore(caller);
    }

    @PostMapping("/email-deliveries/{id}/confirm")
    public ResponseEntity<Void> confirmDelivery(@PathVariable String id, @Valid @RequestBody ConfirmDeliveryRequestDto request,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller) {
        codeSignIn.confirmDelivery(caller, id, request.getEvidence());
        return ResponseEntity.ok().build();
    }
}
