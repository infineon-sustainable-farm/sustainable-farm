/**
 * Product Transformation - dashboard aggregations.
 *
 * Pure functions that turn existing API records into chart-ready rows. They
 * do not invent fields: every number is read from a record the backend already
 * returns (raw intakes, batches, wash & sort records, drying runs).
 *
 * Kept outside the components so the mapping can be tested without rendering
 * a chart, the same way `utils/batchTimeline.js` is.
 */

import { STATUS_CHAIN } from './batchTimeline';

const GRADE_ORDER = ['A', 'B', 'C', 'D'];

/**
 * The quality target used by the backend (`DashboardService` builds a 90 %
 * target for the quality pass rate). It is a business target, not a mock
 * value; the dashboard draws it as a reference line on the trend chart.
 */
export const QUALITY_TARGET_PERCENT = 90;

const asArray = (value) => (Array.isArray(value) ? value : []);

const toNumber = (value) => {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
};

export const hasRows = (rows) => Array.isArray(rows) && rows.length > 0;

/**
 * Grade split of the received mangoes: quantity and number of receptions per
 * grade. Grades outside A-D are kept after the known ones so nothing is lost.
 *
 * @param {Array<Object>} rawIntakes - `/api/raw-intakes` rows.
 * @returns {Array<{ grade: string, quantityKg: number, count: number }>}
 */
export function aggregateGradeDistribution(rawIntakes) {
  const byGrade = new Map();

  for (const intake of asArray(rawIntakes)) {
    const grade = intake?.receivedGrade ? String(intake.receivedGrade).toUpperCase() : null;
    if (!grade) continue;

    const current = byGrade.get(grade) ?? { grade, quantityKg: 0, count: 0 };
    current.quantityKg += toNumber(intake.receivedQuantityKg);
    current.count += 1;
    byGrade.set(grade, current);
  }

  return [...byGrade.values()].sort((a, b) => {
    const aIndex = GRADE_ORDER.indexOf(a.grade);
    const bIndex = GRADE_ORDER.indexOf(b.grade);
    return (aIndex === -1 ? 99 : aIndex) - (bIndex === -1 ? 99 : bIndex);
  });
}

/**
 * Batches per lifecycle stage, in the order of the status chain, plus a
 * REJECTED row when there is at least one rejected batch. Stages with no
 * batches are kept so the pipeline reads as a full line.
 *
 * @param {Array<Object>} batches - `/api/batches` rows.
 * @returns {Array<{ status: string, count: number }>}
 */
export function aggregateBatchStatus(batches) {
  const counts = new Map();

  for (const batch of asArray(batches)) {
    const status = batch?.currentStatus ? String(batch.currentStatus).toUpperCase() : 'CREATED';
    counts.set(status, (counts.get(status) ?? 0) + 1);
  }

  const rows = STATUS_CHAIN.map((status) => ({ status, count: counts.get(status) ?? 0 }));
  const rejected = counts.get('REJECTED') ?? 0;
  if (rejected > 0) rows.push({ status: 'REJECTED', count: rejected });
  return rows;
}

/** The number of batches represented by an aggregateBatchStatus result. */
export const totalBatches = (rows) =>
  asArray(rows).reduce((sum, row) => sum + toNumber(row.count), 0);

/**
 * Per-batch processing performance from the records the line already writes:
 * the wash & sort yield (declared, or output/input when the field is absent)
 * and the drying throughput (wash output kg per drying hour).
 *
 * @param {Array<Object>} washRecords - `/api/wash-sort-records` rows.
 * @param {Array<Object>} dryingRuns - `/api/drying-runs` rows.
 * @returns {Array<Object>} up to eight batches, ordered by batch id.
 */
export function aggregateProcessingPerformance(washRecords, dryingRuns) {
  const byBatch = new Map();

  const ensure = (batchId) => {
    const key = batchId || 'Unassigned';
    if (!byBatch.has(key)) {
      byBatch.set(key, {
        batchId: key,
        outputKg: 0,
        yieldSum: 0,
        yieldCount: 0,
        durationHours: 0,
        energyKwh: 0,
      });
    }
    return byBatch.get(key);
  };

  for (const record of asArray(washRecords)) {
    const row = ensure(record?.batchId);
    const inputKg = toNumber(record?.inputQuantityKg);
    const outputKg = toNumber(record?.outputQuantityKg);
    row.outputKg += outputKg;

    const declaredYield = Number(record?.yieldPercentage);
    if (Number.isFinite(declaredYield)) {
      row.yieldSum += declaredYield;
      row.yieldCount += 1;
    } else if (inputKg > 0) {
      row.yieldSum += (outputKg / inputKg) * 100;
      row.yieldCount += 1;
    }
  }

  for (const run of asArray(dryingRuns)) {
    const row = ensure(run?.batchId);
    row.durationHours += toNumber(run?.durationHours);
    row.energyKwh += toNumber(run?.energyUsageKwh);
  }

  return [...byBatch.values()]
    .map((row) => ({
      batchId: row.batchId,
      yieldPct: row.yieldCount > 0 ? row.yieldSum / row.yieldCount : null,
      durationHours: row.durationHours,
      throughputKgPerHour:
        row.durationHours > 0 && row.outputKg > 0 ? row.outputKg / row.durationHours : null,
      energyKwh: row.energyKwh,
    }))
    .filter((row) => row.yieldPct !== null || row.durationHours > 0)
    .sort((a, b) => String(a.batchId).localeCompare(String(b.batchId)))
    .slice(0, 8);
}

/**
 * Normalises the quality-trend payload to `{ date, passRate }` rows, dropping
 * entries without a usable pass rate so a broken row cannot flatten the line.
 *
 * @param {Array<Object>} rows - `/charts/quality-trend` rows.
 */
export function normalizeQualityTrend(rows) {
  return asArray(rows)
    .filter(
      (row) =>
        row?.date &&
        row?.passRate !== null &&
        row?.passRate !== undefined &&
        row?.passRate !== '',
    )
    .map((row) => ({ date: row.date, passRate: Number(row.passRate) }))
    .filter((row) => Number.isFinite(row.passRate));
}
