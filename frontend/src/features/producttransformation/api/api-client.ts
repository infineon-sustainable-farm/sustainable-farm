import { useAuth } from '@/hooks/useAuth'

// Base API URL - will be configured per environment
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// Helper to handle API responses
async function handleResponse(response) {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new Error(
      errorData.message || `API error: ${response.status} ${response.statusText}`
    )
  }
  return response.json()
}

// Auth headers - include credentials for session cookies
function authHeaders() {
  return {
    'Content-Type': 'application/json',
    ...(useAuth()?.isAuthenticated ? {} : {}),
  }
}

// ============ Batch APIs ============

export interface Batch {
  batchId: string
  harvestDate: string
  mangoVariety: string
  harvestQuantityKg: number
  farmId: string
  blockId: string
  currentStatus: string
  createdAt?: string
  updatedAt?: string
}

export interface BatchCreateRequest {
  harvestDate: string
  mangoVariety: string
  harvestQuantityKg: number
  farmId: string
  blockId: string
}

export interface BatchStatusAdvanceRequest {
  targetStatus: string
}

export const batchApis = {
  // Create new batch
  create: async (data: BatchCreateRequest): Promise<Batch> => {
    const res = await fetch(`${API_BASE_URL}/batches`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get batch by ID
  getById: async (batchId: string): Promise<Batch> => {
    const res = await fetch(`${API_BASE_URL}/batches/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all batches
  getAll: async (): Promise<Batch[]> => {
    const res = await fetch(`${API_BASE_URL}/batches`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update batch
  update: async (batchId: string, data: Partial<Batch>): Promise<Batch> => {
    const res = await fetch(`${API_BASE_URL}/batches/${batchId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Delete batch
  delete: async (batchId: string): Promise<void> => {
    await fetch(`${API_BASE_URL}/batches/${batchId}`, {
      method: 'DELETE',
      credentials: 'include',
    })
  },

  // Advance batch status
  advanceStatus: async (batchId: string): Promise<Batch> => {
    const res = await fetch(`${API_BASE_URL}/batches/${batchId}/advance-status`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get batches by status
  getByStatus: async (status: string): Promise<Batch[]> => {
    const res = await fetch(`${API_BASE_URL}/batches/status/${status}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get batches ready for shipping
  getReadyForShipping: async (): Promise<Batch[]> => {
    const res = await fetch(`${API_BASE_URL}/batches/ready-for-shipping`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get batches by farm
  getByFarm: async (farmId: string): Promise<Batch[]> => {
    const res = await fetch(`${API_BASE_URL}/batches/farm/${farmId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get batches by variety
  getByVariety: async (variety: string): Promise<Batch[]> => {
    const res = await fetch(`${API_BASE_URL}/batches/variety/${variety}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get batches by date range
  getByDateRange: async (startDate: string, endDate: string): Promise<Batch[]> => {
    const res = await fetch(
      `${API_BASE_URL}/batches/date-range?start=${startDate}&end=${endDate}`,
      {
        credentials: 'include',
      }
    )
    return handleResponse(res)
  },
}

// ============ Raw Intake APIs ============

export interface RawIntake {
  intakeId: string
  batchId: string
  sourceFarm: string
  sourceBlock: string
  intakeDate: string
  receivedQuantityKg: number
  receivedVariety: string
  receivedGrade: string
  intakeOperator?: string
  createdAt?: string
  updatedAt?: string
}

export interface RawIntakeCreateRequest {
  sourceFarm: string
  sourceBlock: string
  intakeDate: string
  receivedQuantityKg: number
  receivedVariety: string
  receivedGrade: string
  intakeOperator?: string
}

export const rawIntakeApis = {
  // Create raw intake
  create: async (data: RawIntakeCreateRequest): Promise<RawIntake> => {
    const res = await fetch(`${API_BASE_URL}/raw-intake`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get raw intake by batch ID
  getByBatchId: async (batchId: string): Promise<RawIntake> => {
    const res = await fetch(`${API_BASE_URL}/raw-intake/batch/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all raw intakes
  getAll: async (): Promise<RawIntake[]> => {
    const res = await fetch(`${API_BASE_URL}/raw-intake`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update raw intake
  update: async (intakeId: string, data: Partial<RawIntake>): Promise<RawIntake> => {
    const res = await fetch(`${API_BASE_URL}/raw-intake/${intakeId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Delete raw intake
  delete: async (intakeId: string): Promise<void> => {
    await fetch(`${API_BASE_URL}/raw-intake/${intakeId}`, {
      method: 'DELETE',
      credentials: 'include',
    })
  },
}

// ============ Wash & Sort APIs ============

export interface WashSortRecord {
  recordId: string
  batchId: string
  startTime: string
  endTime?: string
  inputQuantityKg: number
  outputQuantityKg: number
  wasteQuantityKg?: number
  waterUsageLiters?: number
  equipmentId?: string
  operatorId?: string
  createdAt?: string
  updatedAt?: string
}

export interface WashSortCreateRequest {
  batchId: string
  startTime: string
  inputQuantityKg: number
  equipmentId?: string
  operatorId?: string
}

export const washSortApis = {
  // Create wash/sort record
  create: async (data: WashSortCreateRequest): Promise<WashSortRecord> => {
    const res = await fetch(`${API_BASE_URL}/wash-sort-records`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get wash/sort record by batch ID
  getByBatchId: async (batchId: string): Promise<WashSortRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/wash-sort-records/batch/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all wash/sort records
  getAll: async (): Promise<WashSortRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/wash-sort-records`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Complete wash/sort record
  complete: async (recordId: string): Promise<WashSortRecord> => {
    const res = await fetch(`${API_BASE_URL}/wash-sort-records/${recordId}/complete`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Drying Run APIs ============

export interface DryingRun {
  runId: string
  batchId: string
  startTime: string
  endTime?: string
  targetTemperatureC: number
  actualTemperatureC?: number
  startMoisturePct: number
  endMoisturePct?: number
  durationHours?: number
  energyUsageKwh?: number
  equipmentId?: string
  operatorId?: string
  createdAt?: string
  updatedAt?: string
}

export interface DryingRunCreateRequest {
  batchId: string
  startTime: string
  targetTemperatureC: number
  inputQuantityKg?: number
  equipmentId?: string
  operatorId?: string
}

export const dryingRunApis = {
  // Create drying run
  create: async (data: DryingRunCreateRequest): Promise<DryingRun> => {
    const res = await fetch(`${API_BASE_URL}/drying-runs`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get drying runs by batch ID
  getByBatchId: async (batchId: string): Promise<DryingRun[]> => {
    const res = await fetch(`${API_BASE_URL}/drying-runs/batch/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all drying runs
  getAll: async (): Promise<DryingRun[]> => {
    const res = await fetch(`${API_BASE_URL}/drying-runs`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update drying run
  update: async (runId: string, data: Partial<DryingRun>): Promise<DryingRun> => {
    const res = await fetch(`${API_BASE_URL}/drying-runs/${runId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Packaging Record APIs ============

export interface PackagingRecord {
  recordId: string
  batchId: string
  lotCode: string
  packageType: string
  packageQuantityKg: number
  equipmentId?: string
  operatorId?: string
  exportReady: boolean
  packagingDate?: string
  createdAt?: string
  updatedAt?: string
}

export interface PackagingCreateRequest {
  batchId: string
  lotCode: string
  packageType: string
  packageQuantityKg: number
  equipmentId?: string
  operatorId?: string
}

export const packagingApis = {
  // Create packaging record
  create: async (data: PackagingCreateRequest): Promise<PackagingRecord> => {
    const res = await fetch(`${API_BASE_URL}/packaging-records`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get packaging record by batch ID
  getByBatchId: async (batchId: string): Promise<PackagingRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/packaging-records/batch/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all packaging records
  getAll: async (): Promise<PackagingRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/packaging-records`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Toggle export ready status
  toggleExportReady: async (recordId: string): Promise<PackagingRecord> => {
    const res = await fetch(`${API_BASE_URL}/packaging-records/${recordId}/export-ready`, {
      method: 'PATCH',
      headers: {
        ...authHeaders(),
      },
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get export-ready records
  getExportReady: async (): Promise<PackagingRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/packaging-records/export-ready`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ QC Checkpoint APIs ============

export interface QcCheckpoint {
  checkpointId: string
  batchId: string
  stage: string
  result: string
  defects?: string
  defectsCount?: number
  inspectorId?: string
  notes?: string
  checkpointTime: string
  createdAt?: string
  updatedAt?: string
}

export interface QcMandatoryCreateRequest {
  batchId: string
  stage: string
  result: string
  defectsCount?: number
  notes?: string
}

export interface QcCheckpointCreateRequest {
  batchId: string
  stage: string
  result: string
  defects?: string
  defectsCount?: number
  inspectorId?: string
  notes?: string
}

export const qcApis = {
  // Create QC checkpoint
  create: async (data: QcCheckpointCreateRequest): Promise<QcCheckpoint> => {
    const res = await fetch(`${API_BASE_URL}/qc-checkpoints`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get QC checkpoints by batch ID
  getByBatchId: async (batchId: string): Promise<QcCheckpoint[]> => {
    const res = await fetch(`${API_BASE_URL}/qc-checkpoints/batch/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get mandatory QC checkpoints for a batch
  getMandatoryByBatchId: async (batchId: string): Promise<QcCheckpoint[]> => {
    const res = await fetch(`${API_BASE_URL}/qc-checkpoints/mandatory/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get QC checkpoint by ID
  getById: async (checkpointId: string): Promise<QcCheckpoint> => {
    const res = await fetch(`${API_BASE_URL}/qc-checkpoints/${checkpointId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update QC checkpoint
  update: async (checkpointId: string, data: Partial<QcCheckpoint>): Promise<QcCheckpoint> => {
    const res = await fetch(`${API_BASE_URL}/qc-checkpoints/${checkpointId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Delete QC checkpoint
  delete: async (checkpointId: string): Promise<void> => {
    await fetch(`${API_BASE_URL}/qc-checkpoints/${checkpointId}`, {
      method: 'DELETE',
      credentials: 'include',
    })
  },
}

// ============ Compliance Record APIs ============

export interface ComplianceRecord {
  recordId: string
  batchId: string
  complianceType: string
  auditDate?: string
  auditorId?: string
  evidence?: string
  requirement?: string
  result?: string
  nextAuditDate?: string
  createdAt?: string
  updatedAt?: string
}

export interface ComplianceCreateRequest {
  batchId: string
  complianceType: string
  auditDate?: string
  auditorId?: string
  evidence?: string
  requirement?: string
}

export const complianceApis = {
  // Create compliance record
  create: async (data: ComplianceCreateRequest): Promise<ComplianceRecord> => {
    const res = await fetch(`${API_BASE_URL}/compliance-records`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get compliance records by batch ID
  getByBatchId: async (batchId: string): Promise<ComplianceRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/compliance-records/batch/${batchId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all compliance records
  getAll: async (): Promise<ComplianceRecord[]> => {
    const res = await fetch(`${API_BASE_URL}/compliance-records`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update compliance record
  update: async (recordId: string, data: Partial<ComplianceRecord>): Promise<ComplianceRecord> => {
    const res = await fetch(`${API_BASE_URL}/compliance-records/${recordId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Operator APIs ============

export interface Operator {
  operatorId: string
  name: string
  role: string
  status: string
  certification?: string
  createdAt?: string
  updatedAt?: string
}

export interface OperatorCreateRequest {
  name: string
  role: string
  status?: string
  certification?: string
}

export const operatorApis = {
  // Create operator
  create: async (data: OperatorCreateRequest): Promise<Operator> => {
    const res = await fetch(`${API_BASE_URL}/operators`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get operator by ID
  getById: async (operatorId: string): Promise<Operator> => {
    const res = await fetch(`${API_BASE_URL}/operators/${operatorId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all operators
  getAll: async (): Promise<Operator[]> => {
    const res = await fetch(`${API_BASE_URL}/operators`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update operator
  update: async (operatorId: string, data: Partial<Operator>): Promise<Operator> => {
    const res = await fetch(`${API_BASE_URL}/operators/${operatorId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get operators by role
  getByRole: async (role: string): Promise<Operator[]> => {
    const res = await fetch(`${API_BASE_URL}/operators/role/${role}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Equipment APIs ============

export interface Equipment {
  equipmentId: string
  type: string
  status: string
  location?: string
  maintenanceSchedule?: string
  createdAt?: string
  updatedAt?: string
}

export interface EquipmentCreateRequest {
  type: string
  status?: string
  location?: string
  maintenanceSchedule?: string
}

export const equipmentApis = {
  // Create equipment
  create: async (data: EquipmentCreateRequest): Promise<Equipment> => {
    const res = await fetch(`${API_BASE_URL}/equipment`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get equipment by ID
  getById: async (equipmentId: string): Promise<Equipment> => {
    const res = await fetch(`${API_BASE_URL}/equipment/${equipmentId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all equipment
  getAll: async (): Promise<Equipment[]> => {
    const res = await fetch(`${API_BASE_URL}/equipment`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Update equipment
  update: async (equipmentId: string, data: Partial<Equipment>): Promise<Equipment> => {
    const res = await fetch(`${API_BASE_URL}/equipment/${equipmentId}`, {
      method: 'PUT',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get equipment by type
  getByType: async (type: string): Promise<Equipment[]> => {
    const res = await fetch(`${API_BASE_URL}/equipment/type/${type}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Harvest Event APIs ============

export interface HarvestEvent {
  harvestId: string
  farmId: string
  blockCode: string
  varietyId: string
  varietyName: string
  harvestDate: string
  quantityKg: number
  source: string
  lastUpdated: string
}

export interface HarvestEventCreateRequest {
  farmId: string
  blockCode: string
  varietyId: string
  varietyName: string
  harvestDate: string
  quantityKg: number
  source?: string
}

export const harvestEventApis = {
  // Get harvests from Plants integration
  getFromPlants: async (): Promise<HarvestEvent[]> => {
    const res = await fetch(`${API_BASE_URL}/plants/harvests`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Create harvest event
  create: async (data: HarvestEventCreateRequest): Promise<HarvestEvent> => {
    const res = await fetch(`${API_BASE_URL}/harvest-events`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get harvest event by ID
  getById: async (harvestId: string): Promise<HarvestEvent> => {
    const res = await fetch(`${API_BASE_URL}/harvest-events/${harvestId}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all harvest events
  getAll: async (): Promise<HarvestEvent[]> => {
    const res = await fetch(`${API_BASE_URL}/harvest-events`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Historical Harvest APIs ============

export interface HistoricalHarvest {
  year: number
  month: number
  week: number
  mangoVariety: string
  harvestQuantityKg: number
  qualityGradeAPct: number
  qualityGradeBPct: number
  qualityGradeCPct: number
  weatherCondition: string
  rainfallMm: number
  temperatureAvgC: number
  createdAt?: string
  updatedAt?: string
}

export interface HistoricalHarvestCreateRequest {
  year: number
  month: number
  week: number
  mangoVariety: string
  harvestQuantityKg: number
  qualityGradeAPct?: number
  qualityGradeBPct?: number
  qualityGradeCPct?: number
  weatherCondition?: string
  rainfallMm?: number
  temperatureAvgC?: number
}

export const historicalHarvestApis = {
  // Create historical harvest record
  create: async (data: HistoricalHarvestCreateRequest): Promise<HistoricalHarvest> => {
    const res = await fetch(`${API_BASE_URL}/historical-harvest`, {
      method: 'POST',
      headers: {
        ...authHeaders(),
      },
      body: JSON.stringify(data),
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get historical harvests by variety
  getByVariety: async (variety: string): Promise<HistoricalHarvest[]> => {
    const res = await fetch(`${API_BASE_URL}/historical-harvest/variety/${variety}`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get all historical harvests
  getAll: async (): Promise<HistoricalHarvest[]> => {
    const res = await fetch(`${API_BASE_URL}/historical-harvest`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

// ============ Dashboard APIs ============

export interface KPIMetric {
  label: string
  value: string | number
  change?: string
  trend?: 'up' | 'down' | 'neutral'
}

export interface DashboardKPIs {
  totalBatches: number
  activeBatches: number
  completedBatches: number
  rejectedBatches: number
  totalHarvestKg: number
  averageMoisturePct: number
  qualityPassRate: number
}

export interface ChartDataPoint {
  name: string
  y: number
}

export interface QualityTrendData {
  dates: string[]
  passRate: number[]
  rejectRate: number[]
}

export interface EquipmentUtilizationData {
  equipmentId: string
  utilizationPct: number
}

export const dashboardApis = {
  // Get dashboard KPIs
  getKPIs: async (): Promise<DashboardKPIs> => {
    const res = await fetch(`${API_BASE_URL}/product-transformation/dashboard/kpis`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get quality trend data
  getQualityTrend: async (): Promise<QualityTrendData> => {
    const res = await fetch(`${API_BASE_URL}/product-transformation/dashboard/quality-trend`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get equipment utilization data
  getEquipmentUtilization: async (): Promise<EquipmentUtilizationData[]> => {
    const res = await fetch(`${API_BASE_URL}/product-transformation/dashboard/equipment-utilization`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get harvest trend data
  getHarvestTrend: async (): Promise<{ labels: string[]; data: number[] }> => {
    const res = await fetch(`${API_BASE_URL}/product-transformation/dashboard/harvest-trend`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },

  // Get energy consumption data
  getEnergyConsumption: async (): Promise<{ labels: string[]; data: number[] }> => {
    const res = await fetch(`${API_BASE_URL}/product-transformation/dashboard/energy-consumption`, {
      credentials: 'include',
    })
    return handleResponse(res)
  },
}

export default {
  batchApis,
  rawIntakeApis,
  washSortApis,
  dryingRunApis,
  packagingApis,
  qcApis,
  complianceApis,
  operatorApis,
  equipmentApis,
  harvestEventApis,
  historicalHarvestApis,
  dashboardApis,
}