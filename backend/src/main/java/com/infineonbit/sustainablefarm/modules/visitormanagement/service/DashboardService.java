package com.infineonbit.sustainablefarm.modules.visitormanagement.service;

import com.infineonbit.sustainablefarm.modules.visitormanagement.dto.DashboardResponse;

/**
 * Aggregates the KPIs shown on the Visitor Management dashboard. Always
 * reports the current week (Monday to Sunday) so that when a new week starts
 * the figures roll over automatically.
 */
public interface DashboardService {

    DashboardResponse getDashboard();
}