import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../../shared/api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

import { apiClient } from '../../../shared/api/client';
import { batchApi } from './batchApi';
import { complianceApi } from './complianceApi';
import { dashboardApi } from './dashboardApi';
import { dryingApi } from './dryingApi';
import { equipmentApi } from './equipmentApi';
import { operatorApi } from './operatorApi';
import { packagingApi } from './packagingApi';
import { qcApi } from './qcApi';
import { rawIntakeApi } from './rawIntakeApi';
import { washSortApi } from './washSortApi';

beforeEach(() => {
  vi.clearAllMocks();
  apiClient.get.mockResolvedValue([]);
  apiClient.post.mockResolvedValue({});
  apiClient.put.mockResolvedValue({});
  apiClient.patch.mockResolvedValue({});
  apiClient.delete.mockResolvedValue(null);
});

describe('batchApi', () => {
  it('uses the documented batch endpoints', () => {
    batchApi.getAllBatches();
    batchApi.getBatchById('B-1');
    batchApi.createBatch({ batchId: 'B-1' });
    batchApi.updateBatch('B-1', { currentStatus: 'DRYING' });
    batchApi.deleteBatch('B-1');
    batchApi.advanceBatchStatus('B-1');
    batchApi.setBatchStatus('B-1', 'REJECTED');
    batchApi.getBatchesByStatus('DRYING');
    batchApi.getBatchesByFarm('F-1');
    batchApi.getBatchesByVariety('KEITT');
    batchApi.getBatchesReadyForShipping();
    batchApi.getBatchesByFarmAndBlock('F-1', 'BL-1');
    batchApi.getBatchesByHarvestDateRange('2026-10-01', '2026-10-31');

    expect(apiClient.get).toHaveBeenCalledWith('/api/batches');
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/B-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/batches', { batchId: 'B-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/batches/B-1', { currentStatus: 'DRYING' });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/batches/B-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/batches/B-1/advance-status', {});
    expect(apiClient.put).toHaveBeenCalledWith('/api/batches/B-1/status', null, {
      params: { status: 'REJECTED' },
    });
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/status/DRYING');
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/farm/F-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/variety/KEITT');
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/ready-for-shipping');
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/farm/F-1/block/BL-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/batches/date-range', {
      params: { startDate: '2026-10-01', endDate: '2026-10-31' },
    });
  });
});

describe('complianceApi', () => {
  it('uses the documented compliance endpoints', () => {
    complianceApi.getAllComplianceRecords();
    complianceApi.getComplianceRecordById('C-1');
    complianceApi.createComplianceRecord({ recordId: 'C-1' });
    complianceApi.updateComplianceRecord('C-1', { result: 'PENDING' });
    complianceApi.deleteComplianceRecord('C-1');
    complianceApi.getComplianceRecordsByBatch('B-1');
    complianceApi.getComplianceRecordsByType('HACCP');
    complianceApi.getComplianceRecordsByResult('COMPLIANT');
    complianceApi.getComplianceRecordsByAuditor('OP-1');
    complianceApi.getOverdueComplianceRecords();
    complianceApi.getComplianceRecordsByDateRange('2026-10-01', '2026-10-31');

    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records');
    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records/C-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/compliance-records', { recordId: 'C-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/compliance-records/C-1', { result: 'PENDING' });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/compliance-records/C-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records/batch/B-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records/type/HACCP');
    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records/result/COMPLIANT');
    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records/auditor/OP-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/compliance-records/overdue');
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/compliance-records/date-range?startDate=2026-10-01&endDate=2026-10-31',
    );
  });
});

describe('dashboardApi', () => {
  // The client only exposes the endpoints the dashboard screens consume.
  // Several /kpi/* routes exist on the backend but have no client wrapper yet,
  // so this test asserts the surface that actually exists.
  it('uses the documented dashboard endpoints, with optional date filters', async () => {
    await dashboardApi.getDashboardStats();
    await dashboardApi.getHarvestTrendData();
    await dashboardApi.getHarvestTrendData('2026-10-01', '2026-10-31');
    await dashboardApi.getProductionOutputData();
    await dashboardApi.getProductionOutputData('2026-10-01', '2026-10-31');
    await dashboardApi.getEnergyBreakdownData();
    await dashboardApi.getEquipmentUtilizationData();
    await dashboardApi.getQualityTrendData();
    await dashboardApi.getQualityTrendData('2026-10-01', '2026-10-31');

    expect(apiClient.get).toHaveBeenCalledWith('/api/product-transformation/dashboard/kpis');
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/harvest-trend',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/harvest-trend?startDate=2026-10-01&endDate=2026-10-31',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/production-output',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/production-output?startDate=2026-10-01&endDate=2026-10-31',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/energy-breakdown',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/equipment-utilization',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/quality-trend',
    );
    expect(apiClient.get).toHaveBeenCalledWith(
      '/api/product-transformation/dashboard/charts/quality-trend?startDate=2026-10-01&endDate=2026-10-31',
    );
  });
});

describe('dryingApi', () => {
  it('uses the documented drying-run endpoints', () => {
    dryingApi.getAllDryingRuns();
    dryingApi.getDryingRunById('DR-1');
    dryingApi.getDryingRunsByBatch('B-1');
    dryingApi.createDryingRun({ runId: 'DR-1' });
    dryingApi.updateDryingRun('DR-1', { durationHours: 5 });
    dryingApi.deleteDryingRun('DR-1');
    dryingApi.getDryingRunsByEquipment('EQ-1');

    expect(apiClient.get).toHaveBeenCalledWith('/api/drying-runs');
    expect(apiClient.get).toHaveBeenCalledWith('/api/drying-runs/DR-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/drying-runs/batch/B-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/drying-runs', { runId: 'DR-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/drying-runs/DR-1', { durationHours: 5 });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/drying-runs/DR-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/drying-runs/equipment/EQ-1');
  });
});

describe('equipmentApi', () => {
  it('uses the documented PT equipment endpoints', () => {
    equipmentApi.getAllEquipment();
    equipmentApi.getEquipmentById('EQ-1');
    equipmentApi.getAvailableEquipment();
    equipmentApi.getEquipmentByType('SOLAR_DRYER');
    equipmentApi.getEquipmentByStatus('ACTIVE');
    equipmentApi.createEquipment({ equipmentId: 'EQ-1' });
    equipmentApi.updateEquipment('EQ-1', { capacityKgPerHour: 130 });
    equipmentApi.deleteEquipment('EQ-1');

    expect(apiClient.get).toHaveBeenCalledWith('/api/equipment');
    expect(apiClient.get).toHaveBeenCalledWith('/api/equipment/EQ-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/equipment/available');
    expect(apiClient.get).toHaveBeenCalledWith('/api/equipment/type/SOLAR_DRYER');
    expect(apiClient.get).toHaveBeenCalledWith('/api/equipment/status/ACTIVE');
    expect(apiClient.post).toHaveBeenCalledWith('/api/equipment', { equipmentId: 'EQ-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/equipment/EQ-1', { capacityKgPerHour: 130 });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/equipment/EQ-1');
  });
});

describe('operatorApi', () => {
  it('uses the documented operator endpoints', () => {
    operatorApi.getAllOperators();
    operatorApi.getOperatorById('OP-1');
    operatorApi.createOperator({ operatorId: 'OP-1' });
    operatorApi.updateOperator('OP-1', { role: 'WASHER' });
    operatorApi.deleteOperator('OP-1');
    operatorApi.getOperatorsByRole('WASHER');
    operatorApi.getActiveOperators();

    expect(apiClient.get).toHaveBeenCalledWith('/api/operators');
    expect(apiClient.get).toHaveBeenCalledWith('/api/operators/OP-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/operators', { operatorId: 'OP-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/operators/OP-1', { role: 'WASHER' });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/operators/OP-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/operators/role/WASHER');
    expect(apiClient.get).toHaveBeenCalledWith('/api/operators/active');
  });
});

describe('packagingApi', () => {
  it('uses the documented packaging endpoints', () => {
    packagingApi.getAllPackagingRecords();
    packagingApi.getPackagingRecordById('PK-1');
    packagingApi.createPackagingRecord({ recordId: 'PK-1' });
    packagingApi.updatePackagingRecord('PK-1', { exportReady: true });
    packagingApi.deletePackagingRecord('PK-1');
    packagingApi.getPackagingRecordsByBatch('B-1');
    packagingApi.getExportReadyPackagingRecords();
    packagingApi.markExportReady('PK-1');

    expect(apiClient.get).toHaveBeenCalledWith('/api/packaging-records');
    expect(apiClient.get).toHaveBeenCalledWith('/api/packaging-records/PK-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/packaging-records', { recordId: 'PK-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/packaging-records/PK-1', {
      exportReady: true,
    });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/packaging-records/PK-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/packaging-records/batch/B-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/packaging-records/export-ready');
    expect(apiClient.post).toHaveBeenCalledWith('/api/packaging-records/PK-1/mark-export-ready', {});
  });
});

describe('qcApi', () => {
  it('uses the documented QC endpoints', () => {
    qcApi.getAllQcCheckpoints();
    qcApi.getQcCheckpointById('QC-1');
    qcApi.createQcCheckpoint({ checkpointId: 'QC-1' });
    qcApi.updateQcCheckpoint('QC-1', { result: 'PASS' });
    qcApi.deleteQcCheckpoint('QC-1');
    qcApi.getQcCheckpointsByBatch('B-1');
    qcApi.getQcCheckpointsByInspector('OP-1');

    expect(apiClient.get).toHaveBeenCalledWith('/api/qc-checkpoints');
    expect(apiClient.get).toHaveBeenCalledWith('/api/qc-checkpoints/QC-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/qc-checkpoints', { checkpointId: 'QC-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/qc-checkpoints/QC-1', { result: 'PASS' });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/qc-checkpoints/QC-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/qc-checkpoints/batch/B-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/qc-checkpoints/inspector/OP-1');
  });
});

describe('rawIntakeApi', () => {
  it('uses the documented raw-intake endpoints', () => {
    rawIntakeApi.getAllRawIntakes();
    rawIntakeApi.getRawIntakeById('RI-1');
    rawIntakeApi.getRawIntakesByBatch('B-1');
    rawIntakeApi.createRawIntake({ intakeId: 'RI-1' });
    rawIntakeApi.updateRawIntake('RI-1', { receivedQuantityKg: 520 });
    rawIntakeApi.deleteRawIntake('RI-1');
    rawIntakeApi.initializeBatch('RI-1');

    expect(apiClient.get).toHaveBeenCalledWith('/api/raw-intakes');
    expect(apiClient.get).toHaveBeenCalledWith('/api/raw-intakes/RI-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/raw-intakes/batch/B-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/raw-intakes', { intakeId: 'RI-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/raw-intakes/RI-1', {
      receivedQuantityKg: 520,
    });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/raw-intakes/RI-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/raw-intakes/RI-1/initialize-batch');
  });
});

describe('washSortApi', () => {
  it('uses the documented wash & sort endpoints', () => {
    washSortApi.getAllWashSortRecords();
    washSortApi.getWashSortRecordById('WS-1');
    washSortApi.getWashSortRecordsByBatch('B-1');
    washSortApi.createWashSortRecord({ recordId: 'WS-1' });
    washSortApi.updateWashSortRecord('WS-1', { waterUsageLiters: 900 });
    washSortApi.deleteWashSortRecord('WS-1');
    washSortApi.completeWashSortRecord('WS-1', { outputQuantityKg: 480 });

    expect(apiClient.get).toHaveBeenCalledWith('/api/wash-sort-records');
    expect(apiClient.get).toHaveBeenCalledWith('/api/wash-sort-records/WS-1');
    expect(apiClient.get).toHaveBeenCalledWith('/api/wash-sort-records/batch/B-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/wash-sort-records', { recordId: 'WS-1' });
    expect(apiClient.put).toHaveBeenCalledWith('/api/wash-sort-records/WS-1', {
      waterUsageLiters: 900,
    });
    expect(apiClient.delete).toHaveBeenCalledWith('/api/wash-sort-records/WS-1');
    expect(apiClient.post).toHaveBeenCalledWith('/api/wash-sort-records/WS-1/complete', {
      outputQuantityKg: 480,
    });
  });
});
