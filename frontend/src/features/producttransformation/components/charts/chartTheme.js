/**
 * Shared visual constants for the Product Transformation dashboard charts.
 * Keeping them here means every chart uses the same grid, tick and tooltip
 * treatment, which is the main visual inconsistency the audit found.
 */

export const CHART_AXIS_TICK = { fill: '#94a3b8', fontSize: 12 };

export const CHART_TOOLTIP_STYLE = {
  borderRadius: 12,
  border: '1px solid #e5e7eb',
  fontSize: 12,
};

export const CHART_GRID_STROKE = '#eef2f1';

export const CHART_LEGEND_STYLE = { fontSize: 12 };

export const CHART_HEIGHT = 288;
