package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AnyAccount;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityAccessInterceptor;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.ChangePasswordRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.SetupPasswordRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.AuthMessageDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService.PasswordRejectedException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService.WrongSecretException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SignInThrottle;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A signed-in account manages its own password. */
@RestController
@RequestMapping("/api/v1/sitesecurity/account")
@RequiredArgsConstructor
@AnyAccount
public class SiteSecurityAccountController {

    private final AccountAuthService accountAuth;

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller,
            @CookieValue(name = SessionService.COOKIE_NAME, required = false) String session,
            @CookieValue(name = SignInThrottle.DEVICE_COOKIE, required = false) String device) {
        accountAuth.changePassword(caller.operatorId(), request.getCurrentPassword(), request.getNewPassword(), session, device);
        return ResponseEntity.noContent().build();
    }

    /** Migration stage 1: first password of an account signed in with an email code, with a fresh code. */
    @PostMapping("/password/setup")
    public ResponseEntity<Void> setupPassword(@Valid @RequestBody SetupPasswordRequestDto request,
            @RequestAttribute(SiteSecurityAccessInterceptor.PRINCIPAL_ATTRIBUTE) SiteSecurityPrincipal caller,
            @CookieValue(name = SessionService.COOKIE_NAME, required = false) String session) {
        accountAuth.setupPassword(caller.operatorId(), request.getCode(), request.getPassword(), session);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(com.infineonbit.sustainablefarm.modules.sitesecurity.service.PasswordHasher.HashingBusyException.class)
    public ResponseEntity<AuthMessageDto> busy(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new AuthMessageDto(e.getMessage()));
    }

    @ExceptionHandler(PasswordRejectedException.class)
    public ResponseEntity<Map<String, Object>> passwordRejected(PasswordRejectedException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(),
                "fieldErrors", Map.of("password", e.getMessage())));
    }

    @ExceptionHandler(WrongSecretException.class)
    public ResponseEntity<AuthMessageDto> wrongSecret(WrongSecretException e) {
        return ResponseEntity.badRequest().body(new AuthMessageDto(e.getMessage()));
    }
}
