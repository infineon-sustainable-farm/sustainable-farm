package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.AuthMessageDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.RequestLoginCodeDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.SessionDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.VerifyLoginCodeDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LoginCodeService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService.SignIn;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints: they are how an operator proves who they are, and how they leave. */
@RestController
@RequestMapping("/api/v1/sitesecurity/auth")
@RequiredArgsConstructor
public class SiteSecurityAuthController {

    private static final String CODE_REQUESTED = "If this email is allowed, a code has been sent.";
    private static final String CODE_REFUSED = "This code is invalid or has expired.";

    private final LoginCodeService loginCodeService;
    private final SessionService sessionService;

    @PostMapping("/request-code")
    public ResponseEntity<AuthMessageDto> requestCode(@Valid @RequestBody RequestLoginCodeDto request) {
        loginCodeService.requestCode(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AuthMessageDto(CODE_REQUESTED));
    }

    /** A right code opens an 8-hour session, carried by an HttpOnly cookie. */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyLoginCodeDto request) {
        Optional<SignIn> signIn = sessionService.signIn(request.getEmail(), request.getCode());
        if (signIn.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthMessageDto(CODE_REFUSED));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionService.sessionCookie(signIn.get().token()).toString())
                .body(SessionDto.of(signIn.get().principal()));
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
}
