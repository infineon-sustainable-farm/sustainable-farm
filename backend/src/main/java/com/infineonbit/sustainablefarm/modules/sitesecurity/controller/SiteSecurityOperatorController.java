package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AdminOnly;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.ChangeOperatorRoleRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.CreateOperatorRequestDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.OperatorDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.OperatorService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sitesecurity/operators")
@RequiredArgsConstructor
@AdminOnly
public class SiteSecurityOperatorController {

    /** Until operators can sign in, the shared admin token is the only known actor. */
    static final String ADMIN_TOKEN_ACTOR = "admin-token";

    private final OperatorService operatorService;

    @GetMapping
    public ResponseEntity<List<OperatorDto>> getOperators() {
        return ResponseEntity.ok(operatorService.getOperators());
    }

    @PostMapping
    public ResponseEntity<OperatorDto> addOperator(@Valid @RequestBody CreateOperatorRequestDto request) {
        OperatorDto created = operatorService.addOperator(request, ADMIN_TOKEN_ACTOR);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<OperatorDto> changeRole(
            @PathVariable String id, @Valid @RequestBody ChangeOperatorRoleRequestDto request) {
        return ResponseEntity.ok(operatorService.changeRole(id, request.getRole()));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<OperatorDto> disableOperator(@PathVariable String id) {
        return ResponseEntity.ok(operatorService.disableOperator(id, ADMIN_TOKEN_ACTOR));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<OperatorDto> enableOperator(@PathVariable String id) {
        return ResponseEntity.ok(operatorService.enableOperator(id));
    }
}
