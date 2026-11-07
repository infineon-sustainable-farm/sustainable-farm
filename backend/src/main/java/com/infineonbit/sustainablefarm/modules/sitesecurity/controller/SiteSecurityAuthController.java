package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.AuthMessageDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.RequestLoginCodeDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.VerifyLoginCodeDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LoginCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public endpoints: they are how an operator proves who they are. */
@RestController
@RequestMapping("/api/v1/sitesecurity/auth")
@RequiredArgsConstructor
public class SiteSecurityAuthController {

    private static final String CODE_REQUESTED = "If this email is allowed, a code has been sent.";
    private static final String CODE_REFUSED = "This code is invalid or has expired.";
    private static final String CODE_ACCEPTED = "Code accepted.";

    private final LoginCodeService loginCodeService;

    @PostMapping("/request-code")
    public ResponseEntity<AuthMessageDto> requestCode(@Valid @RequestBody RequestLoginCodeDto request) {
        loginCodeService.requestCode(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AuthMessageDto(CODE_REQUESTED));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthMessageDto> verifyCode(@Valid @RequestBody VerifyLoginCodeDto request) {
        if (loginCodeService.verifyCode(request.getEmail(), request.getCode())) {
            return ResponseEntity.ok(new AuthMessageDto(CODE_ACCEPTED));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthMessageDto(CODE_REFUSED));
    }
}
