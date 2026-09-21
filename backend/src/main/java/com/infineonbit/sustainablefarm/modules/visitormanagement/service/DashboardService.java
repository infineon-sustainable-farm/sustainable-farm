package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.DashboardResponse;

/**
 * Aggregates the KPIs shown on the Visitor Management dashboard.
 */
public interface DashboardService {

    DashboardResponse getDashboard();
}