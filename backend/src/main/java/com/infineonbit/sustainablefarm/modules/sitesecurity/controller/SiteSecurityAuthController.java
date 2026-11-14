package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.EmailOnlyRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.LoginRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.TokenInfoDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.TokenPasswordRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.TokenRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.AuthMessageDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.RequestLoginCodeDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.SessionDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.VerifyLoginCodeDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService.InvalidLinkException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService.PasswordRejectedException;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.CodeSignInSettings;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LoginCodeService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService.SignIn;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.ClientAddress;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountAuthService.PasswordSignIn;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SignInThrottle;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SignInThrottle.ThrottledException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints: signing in and out, opening invitation and reset links.
 * Every refusal is generic, so that nobody can learn which emails have an account.
 */
@RestController
@RequestMapping("/api/v1/sitesecurity/auth")
@RequiredArgsConstructor
public class SiteSecurityAuthController {

    private static final String CODE_REQUESTED = "If this email is allowed, a code has been sent.";
    private static final String CODE_REFUSED = "This code is invalid or has expired.";
    private static final String LOGIN_REFUSED = "Email or password is incorrect.";
    private static final String RESET_REQUESTED = "If an account uses this email, a reset link has been sent.";

    private final LoginCodeService loginCodeService;
    private final SessionService sessionService;
    private final AccountAuthService accountAuth;
    private final CodeSignInSettings codeSignInSettings;
    private final SignInThrottle throttle;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request,
            @CookieValue(name = SessionService.COOKIE_NAME, required = false) String session,
            @CookieValue(name = SignInThrottle.DEVICE_COOKIE, required = false) String device,
            HttpServletRequest http) {
        Optional<PasswordSignIn> signIn = accountAuth.login(request.getEmail(), request.getPassword(), session,
                client(http, device));
        if (signIn.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthMessageDto(LOGIN_REFUSED));
        }
        return signedIn(signIn.get().signIn(), signIn.get().deviceToken());
    }

    /** Legacy email code (migration stages 0 to 2): only for accounts without a password. */
    @PostMapping("/request-code")
    public ResponseEntity<AuthMessageDto> requestCode(@Valid @RequestBody RequestLoginCodeDto request) {
        loginCodeService.requestCode(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AuthMessageDto(CODE_REQUESTED));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyLoginCodeDto request,
            @CookieValue(name = SessionService.COOKIE_NAME, required = false) String session) {
        Optional<SignIn> signIn = sessionService.signInWithCode(request.getEmail(), request.getCode(), session);
        if (signIn.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthMessageDto(CODE_REFUSED));
        }
        return signedIn(signIn.get());
    }

    /** Always succeeds, so that a browser with a stale cookie can still clear it. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = SessionService.COOKIE_NAME, required = false) String session) {
        sessionService.signOut(session);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionService.clearedCookie().toString())
                .build();
    }

    @PostMapping("/tokens/inspect")
    public ResponseEntity<TokenInfoDto> inspect(@Valid @RequestBody TokenRequestDto request) {
        return ResponseEntity.ok(accountAuth.inspect(request.getToken()));
    }

    @PostMapping("/invitations/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody TokenRequestDto request) {
        accountAuth.confirmInvitation(request.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@Valid @RequestBody TokenPasswordRequestDto request,
            @CookieValue(name = SessionService.COOKIE_NAME, required = false) String session,
            @CookieValue(name = SignInThrottle.DEVICE_COOKIE, required = false) String device,
            HttpServletRequest http) {
        PasswordSignIn signIn = accountAuth.activate(request.getToken(), request.getPassword(), session, client(http, device));
        return signedIn(signIn.signIn(), signIn.deviceToken());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthMessageDto> forgotPassword(@Valid @RequestBody EmailOnlyRequestDto request) {
        accountAuth.requestPasswordReset(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AuthMessageDto(RESET_REQUESTED));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody TokenPasswordRequestDto request,
            @CookieValue(name = SignInThrottle.DEVICE_COOKIE, required = false) String device,
            HttpServletRequest http) {
        String deviceToken = accountAuth.resetPassword(request.getToken(), request.getPassword(), client(http, device));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, throttle.deviceCookie(deviceToken).toString())
                .build();
    }

    /** Sign-in limits: before any password work, the same for every email. */
    @ExceptionHandler(ThrottledException.class)
    public ResponseEntity<AuthMessageDto> throttled(ThrottledException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(e.getRetryAfterSeconds()))
                .body(new AuthMessageDto(e.getMessage()));
    }

    @ExceptionHandler(InvalidLinkException.class)
    public ResponseEntity<AuthMessageDto> invalidLink(InvalidLinkException e) {
        return ResponseEntity.badRequest().body(new AuthMessageDto(e.getMessage()));
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

    private ResponseEntity<SessionDto> signedIn(SignIn signIn) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionService.sessionCookie(signIn).toString())
                .body(SessionDto.of(signIn.principal(), codeSignInSettings.isRetired()));
    }

    private ResponseEntity<SessionDto> signedIn(SignIn signIn, String deviceToken) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionService.sessionCookie(signIn).toString())
                .header(HttpHeaders.SET_COOKIE, throttle.deviceCookie(deviceToken).toString())
                .body(SessionDto.of(signIn.principal(), codeSignInSettings.isRetired()));
    }

    private static SignInThrottle.Client client(HttpServletRequest http, String device) {
        return new SignInThrottle.Client(ClientAddress.of(http), device);
    }
}
