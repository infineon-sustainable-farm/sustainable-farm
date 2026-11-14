import { describe, expect, it } from 'vitest';
import {
  QUALITY_TARGET_PERCENT,
  aggregateBatchStatus,
  aggregateGradeDistribution,
  aggregateProcessingPerformance,
  hasRows,
  normalizeQualityTrend,
  totalBatches,
} from './dashboardMetrics';

describe('dashboardMetrics', () => {
  it('aggregates the received quantity and receptions per grade, in A-D order', () => {
    const rows = aggregateGradeDistribution([
      { receivedGrade: 'B', receivedQuantityKg: '80' },
      { receivedGrade: 'A', receivedQuantityKg: 120 },
      { receivedGrade: 'A', receivedQuantityKg: 30 },
      { receivedGrade: 'C', receivedQuantityKg: 10 },
      { receivedGrade: null, receivedQuantityKg: 999 },
    ]);

    expect(rows).toEqual([
      { grade: 'A', quantityKg: 150, count: 2 },
      { grade: 'B', quantityKg: 80, count: 1 },
      { grade: 'C', quantityKg: 10, count: 1 },
    ]);
  });

  it('keeps unknown grades after the known ones instead of dropping them', () => {
    const rows = aggregateGradeDistribution([
      { receivedGrade: 'E', receivedQuantityKg: 5 },
      { receivedGrade: 'A', receivedQuantityKg: 10 },
    ]);

    expect(rows.map((row) => row.grade)).toEqual(['A', 'E']);
  });

  it('orders the batch statuses along the lifecycle and appends REJECTED only when used', () => {
    const rows = aggregateBatchStatus([
      { currentStatus: 'WASHING' },
      { currentStatus: 'WASHING' },
      { currentStatus: 'SHIPPED' },
      { currentStatus: null },
    ]);

    expect(rows).toEqual([
      { status: 'CREATED', count: 1 },
      { status: 'INTAKE', count: 0 },
      { status: 'WASHING', count: 2 },
      { status: 'DRYING', count: 0 },
      { status: 'PACKAGING', count: 0 },
      { status: 'COMPLETED', count: 0 },
      { status: 'SHIPPED', count: 1 },
    ]);
    expect(totalBatches(rows)).toBe(4);

    const withRejected = aggregateBatchStatus([
      { currentStatus: 'REJECTED' },
      { currentStatus: 'CREATED' },
    ]);
    expect(withRejected.at(-1)).toEqual({ status: 'REJECTED', count: 1 });
  });

  it('derives wash yield and drying throughput per batch', () => {
    const rows = aggregateProcessingPerformance(
      [
        { batchId: 'B-1', inputQuantityKg: 120, outputQuantityKg: 100, yieldPercentage: 83.3 },
        { batchId: 'B-2', inputQuantityKg: 50, outputQuantityKg: 40 },
      ],
      [
        { batchId: 'B-1', durationHours: 10, energyUsageKwh: 30 },
        { batchId: 'B-2', durationHours: 5, energyUsageKwh: 12 },
      ],
    );

    expect(rows).toEqual([
      {
        batchId: 'B-1',
        yieldPct: 83.3,
        durationHours: 10,
        throughputKgPerHour: 10,
        energyKwh: 30,
      },
      {
        batchId: 'B-2',
        yieldPct: 80,
        durationHours: 5,
        throughputKgPerHour: 8,
        energyKwh: 12,
      },
    ]);
  });

  it('keeps a wash-only batch and reports its yield without a throughput', () => {
    const rows = aggregateProcessingPerformance(
      [{ batchId: 'B-3', inputQuantityKg: 10, outputQuantityKg: 9, yieldPercentage: 90 }],
      [],
    );

    expect(rows).toEqual([
      {
        batchId: 'B-3',
        yieldPct: 90,
        durationHours: 0,
        throughputKgPerHour: null,
        energyKwh: 0,
      },
    ]);
  });

  it('normalises the quality trend and drops unusable rows', () => {
    const rows = normalizeQualityTrend([
      { date: '2026-11-01', passRate: '92.5' },
      { date: '2026-11-02', passRate: null },
      { date: '', passRate: 80 },
      null,
    ]);

    expect(rows).toEqual([{ date: '2026-11-01', passRate: 92.5 }]);
    expect(QUALITY_TARGET_PERCENT).toBe(90);
  });

  it('treats non-arrays as empty and reports whether rows exist', () => {
    expect(aggregateGradeDistribution(null)).toEqual([]);
    expect(aggregateBatchStatus(undefined).length).toBe(7);
    expect(aggregateProcessingPerformance(null, null)).toEqual([]);
    expect(normalizeQualityTrend('nope')).toEqual([]);
    expect(hasRows([{ id: 1 }])).toBe(true);
    expect(hasRows([])).toBe(false);
    expect(hasRows(null)).toBe(false);
  });
});
