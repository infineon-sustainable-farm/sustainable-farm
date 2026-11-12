// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the other Product Transformation tests).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import BatchDetail from './BatchDetail';
import { batchApi } from '../api/batchApi';
import { rawIntakeApi } from '../api/rawIntakeApi';
import { washSortApi } from '../api/washSortApi';
import { dryingApi } from '../api/dryingApi';
import { qcApi } from '../api/qcApi';
import { packagingApi } from '../api/packagingApi';
import { complianceApi } from '../api/complianceApi';

vi.mock('../api/batchApi', () => ({ batchApi: { getBatchById: vi.fn() } }));
vi.mock('../api/rawIntakeApi', () => ({ rawIntakeApi: { getRawIntakesByBatch: vi.fn() } }));
vi.mock('../api/washSortApi', () => ({ washSortApi: { getWashSortRecordsByBatch: vi.fn() } }));
vi.mock('../api/dryingApi', () => ({ dryingApi: { getDryingRunsByBatch: vi.fn() } }));
vi.mock('../api/qcApi', () => ({ qcApi: { getQcCheckpointsByBatch: vi.fn() } }));
vi.mock('../api/packagingApi', () => ({ packagingApi: { getPackagingRecordsByBatch: vi.fn() } }));
vi.mock('../api/complianceApi', () => ({
  complianceApi: { getComplianceRecordsByBatch: vi.fn() },
}));

afterEach(cleanup);

const BATCH = {
  batchId: 'LOT-DEMO-003',
  harvestDate: '2026-11-09',
  mangoVariety: 'AMÉLIE',
  harvestQuantityKg: 610.25,
  currentStatus: 'PACKAGING',
  farmId: 'FERME-DEMO',
  blockId: 'BLOC-B',
  updatedAt: '2026-11-12T14:13:17.597Z',
};

const INTAKES = [
  {
    intakeId: 'RI-DEMO-003',
    intakeDate: '2026-11-09',
    receivedQuantityKg: 610.25,
    receivedVariety: 'AMÉLIE',
    receivedGrade: 'B',
    sourceFarm: 'FERME-DEMO',
    sourceBlock: 'BLOC-B',
    intakeOperator: 'Moussa DEMO (Lavage)',
  },
];

const WASH = [
  {
    recordId: 'WS-DEMO-002',
    inputQuantityKg: 610.25,
    outputQuantityKg: 565.0,
    wasteQuantityKg: 45.25,
    waterUsageLiters: 1380.0,
    startTime: '2026-11-09T08:15:00',
    endTime: '2026-11-09T10:15:00',
    equipmentId: 'EQ-DEMO-02',
    operatorId: 'OP-DEMO-02',
    yieldPercentage: 92.59,
  },
];

const DRYING = [
  {
    runId: 'DR-DEMO-003',
    durationHours: 7,
    targetTemperatureC: 55,
    actualTemperatureC: 54.8,
    startMoisturePct: 80,
    endMoisturePct: 13,
    energyUsageKwh: 142.1,
    startTime: '2026-11-09T08:00:00',
    endTime: '2026-11-09T15:00:00',
    equipmentId: 'EQ-DEMO-01',
  },
];

const QC = [
  {
    checkpointId: 'QC-DEMO-003',
    stage: 'DRYING',
    result: 'REWORK',
    defectsCount: 5,
    checkpointTime: '2026-11-09T09:30:00',
  },
  {
    checkpointId: 'QC-DEMO-004',
    stage: 'DRYING',
    result: 'PASS',
    defectsCount: 0,
    checkpointTime: '2026-11-09T15:30:00',
  },
];

const PACKAGING = [
  {
    recordId: 'PKG-DEMO-001',
    packageType: 'TWO_KG_BAG',
    packageQuantityKg: 565.0,
    lotCode: 'LOT-DEMO-003-PKG',
    exportReady: true,
    packagingDate: '2026-11-09',
    equipmentId: 'EQ-DEMO-03',
  },
];

const COMPLIANCE = [
  {
    recordId: 'CR-DEMO-003',
    complianceType: 'TRACEABILITY',
    requirement: 'Tracabilite amont/aval',
    result: 'PENDING',
    auditDate: '2026-11-09',
    nextAuditDate: null,
  },
];

const renderDetail = (batchId = 'LOT-DEMO-003') =>
  render(
    <MemoryRouter initialEntries={[`/product-transformation/batches/${batchId}`]}>
      <Routes>
        <Route path="/product-transformation/batches/:batchId" element={<BatchDetail />} />
        <Route path="/product-transformation/batches" element={<p>batches list</p>} />
      </Routes>
    </MemoryRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  batchApi.getBatchById.mockResolvedValue({ ...BATCH });
  rawIntakeApi.getRawIntakesByBatch.mockResolvedValue(INTAKES.map((item) => ({ ...item })));
  washSortApi.getWashSortRecordsByBatch.mockResolvedValue(WASH.map((item) => ({ ...item })));
  dryingApi.getDryingRunsByBatch.mockResolvedValue(DRYING.map((item) => ({ ...item })));
  qcApi.getQcCheckpointsByBatch.mockResolvedValue(QC.map((item) => ({ ...item })));
  packagingApi.getPackagingRecordsByBatch.mockResolvedValue(PACKAGING.map((item) => ({ ...item })));
  complianceApi.getComplianceRecordsByBatch.mockResolvedValue(
    COMPLIANCE.map((item) => ({ ...item })),
  );
});

describe('BatchDetail', () => {
  it('renders the seven traceability steps for a complete batch', async () => {
    renderDetail();

    expect(await screen.findByText('LOT-DEMO-003')).toBeTruthy();

    const titles = screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent);
    expect(titles).toEqual([
      'Reception',
      'Washing & sorting',
      'Drying',
      'Quality control',
      'Packaging',
      'Compliance',
      'Batch status',
    ]);
  });

  it('shows the key data of each step', async () => {
    renderDetail();
    await screen.findByText('LOT-DEMO-003');

    expect(screen.getByText('Grade B')).toBeTruthy();
    expect(screen.getByText('Moussa DEMO (Lavage)')).toBeTruthy();
    expect(screen.getByText('565.00 kg · 92.59 % yield')).toBeTruthy();
    expect(screen.getByText('1380.00 L')).toBeTruthy();
    expect(screen.getByText('80.00 % → 13.00 %')).toBeTruthy();
    expect(screen.getByText('54.80 °C (target 55.00 °C)')).toBeTruthy();
    expect(screen.getByText('REWORK · 5 defect(s)')).toBeTruthy();
    expect(screen.getByText('Two Kg Bag')).toBeTruthy();
    expect(screen.getByText('LOT-DEMO-003-PKG')).toBeTruthy();
    expect(screen.getByText('Tracabilite amont/aval')).toBeTruthy();
    expect(screen.getByText('step 5 of 7')).toBeTruthy();
    // Summary in the header: six module records + the batch status itself.
    expect(screen.getByText('7 of 7 steps recorded')).toBeTruthy();
  });

  it('accepts the single-object payload returned by the raw-intake endpoint', async () => {
    // Regression: /api/raw-intakes/batch/{id} answers with one object, not an array,
    // unlike the other five batch endpoints.
    rawIntakeApi.getRawIntakesByBatch.mockResolvedValue({ ...INTAKES[0] });

    renderDetail();
    await screen.findByText('LOT-DEMO-003');

    expect(screen.getByText('Grade B')).toBeTruthy();
    expect(screen.getByText('7 of 7 steps recorded')).toBeTruthy();
    expect(screen.queryByText('Not recorded yet')).toBeNull();
  });

  it('asks every module for that batch, not for the whole table', async () => {
    renderDetail();
    await screen.findByText('LOT-DEMO-003');

    for (const api of [
      rawIntakeApi,
      washSortApi,
      dryingApi,
      qcApi,
      packagingApi,
      complianceApi,
    ]) {
      const spy = Object.values(api)[0];
      expect(spy).toHaveBeenCalledWith('LOT-DEMO-003');
    }
  });

  it('shows "Not recorded yet" instead of an error for a fresh batch', async () => {
    batchApi.getBatchById.mockResolvedValue({
      ...BATCH,
      currentStatus: 'CREATED',
    });
    // The raw-intake endpoint answers 404 when a batch has no reception yet.
    rawIntakeApi.getRawIntakesByBatch.mockRejectedValue(
      Object.assign(new Error('Raw intake not found for batch ID: LOT-DEMO-003'), { status: 404 }),
    );
    washSortApi.getWashSortRecordsByBatch.mockResolvedValue([]);
    dryingApi.getDryingRunsByBatch.mockResolvedValue([]);
    qcApi.getQcCheckpointsByBatch.mockResolvedValue([]);
    packagingApi.getPackagingRecordsByBatch.mockResolvedValue([]);
    complianceApi.getComplianceRecordsByBatch.mockResolvedValue([]);

    renderDetail();
    await screen.findByText('LOT-DEMO-003');

    // Six module steps are empty; the batch status step always has data.
    expect(screen.getAllByText('Not recorded yet')).toHaveLength(6);
    expect(screen.getAllByText('No record for this batch yet.')).toHaveLength(6);
    expect(screen.getByText('1 of 7 steps recorded')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Try again' })).toBeNull();
  });

  it('still reports a real reception failure instead of hiding it', async () => {
    rawIntakeApi.getRawIntakesByBatch.mockRejectedValue(new Error('Database is down'));

    renderDetail();

    expect(await screen.findByText('Database is down')).toBeTruthy();
    expect(screen.queryByText('Not recorded yet')).toBeNull();
  });

  it('links back to the batch list', async () => {
    renderDetail();
    await screen.findByText('LOT-DEMO-003');

    const backLink = screen.getByRole('link', { name: /back to batches/i });
    expect(backLink.getAttribute('href')).toBe('/product-transformation/batches');

    fireEvent.click(backLink);
    expect(await screen.findByText('batches list')).toBeTruthy();
  });

  it('shows the API error and retries', async () => {
    batchApi.getBatchById.mockRejectedValueOnce(new Error('Batch not found'));

    renderDetail('LOT-INCONNU');

    expect(await screen.findByText('Batch not found')).toBeTruthy();

    batchApi.getBatchById.mockResolvedValue({ ...BATCH });
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('LOT-DEMO-003')).toBeTruthy();
    expect(batchApi.getBatchById).toHaveBeenCalledTimes(2);
  });

  it('reads the batch id from the URL', async () => {
    renderDetail('LOT-XYZ');

    await screen.findByRole('heading', { level: 1 });
    expect(batchApi.getBatchById).toHaveBeenCalledWith('LOT-XYZ');
  });
});
