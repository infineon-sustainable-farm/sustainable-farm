package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.SiteSecurityDtos.*;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SiteSecurityService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/sitesecurity", "/api/sitesecurity"})
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class SiteSecurityController {

    private final SiteSecurityService siteSecurityService;

    @GetMapping("/overview")
    public ResponseEntity<OverviewResponseDto> getOverview() {
        return ResponseEntity.ok(siteSecurityService.getOverview());
    }

    @GetMapping("/zones")
    public ResponseEntity<List<ZoneDto>> getZones() {
        return ResponseEntity.ok(siteSecurityService.getZones());
    }

    @GetMapping("/credentials")
    public ResponseEntity<List<UserDto>> getCredentials() {
        return ResponseEntity.ok(siteSecurityService.getCredentials());
    }

    @PostMapping("/credentials")
    public ResponseEntity<UserDto> createCredential(@RequestBody CreateUserRequestDto request) {
        UserDto created = siteSecurityService.createCredential(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntryDto>> getAccessLogs(@RequestParam(required = false, defaultValue = "all") String filter) {
        return ResponseEntity.ok(siteSecurityService.getAccessLogs(filter));
    }
}
