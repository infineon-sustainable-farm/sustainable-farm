/**
 * Product Transformation - dashboard demo data.
 *
 * SYNTHETIC DATA. Nothing in this file comes from the backend and nothing in
 * this file is written to it. It exists so the dashboard can be demonstrated
 * before the production line has recorded enough batches, and it is only used
 * when BOTH of these are true:
 *
 *   1. the build sets VITE_DEMO_DATA="true"; and
 *   2. the matching real API call returned no rows.
 *
 * Every card that falls back to this data renders a visible "Demo data" badge,
 * and the dashboard shows the demo banner. Mock data is kept out of `api/` so
 * an API client can never return it by accident.
 *
 * The shapes match the aggregation helpers in `utils/dashboardMetrics.js`.
 */

export const DEMO_DATA_LABEL = 'Demo data';

/** Grade split of received mangoes (kg), by the PT raw-intake grade. */
export const DEMO_GRADE_DISTRIBUTION = [
  { grade: 'A', quantityKg: 4820, count: 12 },
  { grade: 'B', quantityKg: 2140, count: 8 },
  { grade: 'C', quantityKg: 760, count: 4 },
  { grade: 'D', quantityKg: 180, count: 2 },
];

/** Batches per lifecycle stage, in the order of `STATUS_CHAIN`. */
export const DEMO_BATCH_STATUS = [
  { status: 'CREATED', count: 2 },
  { status: 'INTAKE', count: 1 },
  { status: 'WASHING', count: 3 },
  { status: 'DRYING', count: 2 },
  { status: 'PACKAGING', count: 1 },
  { status: 'COMPLETED', count: 4 },
  { status: 'SHIPPED', count: 3 },
];

/** Per-batch wash yield and drying throughput. */
export const DEMO_PROCESSING_PERFORMANCE = [
  { batchId: 'LOT-DEMO-001', yieldPct: 82.4, durationHours: 18.5, throughputKgPerHour: 12.8 },
  { batchId: 'LOT-DEMO-002', yieldPct: 79.1, durationHours: 21.0, throughputKgPerHour: 10.4 },
  { batchId: 'LOT-DEMO-003', yieldPct: 86.7, durationHours: 16.0, throughputKgPerHour: 14.9 },
  { batchId: 'LOT-DEMO-004', yieldPct: 74.3, durationHours: 24.5, throughputKgPerHour: 8.6 },
  { batchId: 'LOT-DEMO-005', yieldPct: 88.2, durationHours: 15.0, throughputKgPerHour: 16.1 },
  { batchId: 'LOT-DEMO-006', yieldPct: 81.0, durationHours: 19.5, throughputKgPerHour: 11.7 },
];

/** QC pass rate per day, used only when the quality-trend endpoint is empty. */
export const DEMO_QUALITY_TREND = [
  { date: '2026-11-01', passRate: 88.2 },
  { date: '2026-11-02', passRate: 90.1 },
  { date: '2026-11-03', passRate: 89.4 },
  { date: '2026-11-04', passRate: 92.6 },
  { date: '2026-11-05', passRate: 91.3 },
  { date: '2026-11-06', passRate: 93.8 },
  { date: '2026-11-07', passRate: 92.5 },
];
