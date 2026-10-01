package com.infineonbit.sustainablefarm.modules.visitormanagement.controller;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.DashboardResponse;
import com.infineonbit.sustainablefarm.modules.visitormanagement.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }
}