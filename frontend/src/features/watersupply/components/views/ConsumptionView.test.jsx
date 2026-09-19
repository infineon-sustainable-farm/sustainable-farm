import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ConsumptionView } from './ConsumptionView'
import { waterConsumptionApi, waterSourceApi, waterQuotaApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  waterConsumptionApi: {
    getConsumptions: vi.fn(),
  },
  waterSourceApi: {
    getSources: vi.fn(),
  },
  waterQuotaApi: {
    getUsage: vi.fn(),
    createQuota: vi.fn(),
    deleteQuota: vi.fn(),
  },
  downloadCsv: vi.fn(),
}))

vi.mock('../../hooks/useFarms', () => ({
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'North Farm' }] }),
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'South Zone' }] }),
}))

vi.mock('../charts', () => ({
  WsAreaChart: () => <div data-testid="area-chart" />,
  WsBulletChart: () => <div data-testid="bullet-chart" />,
  C: { primary: '#0a8276' },
}))

const CONSUMPTIONS = [
  { id: 'c1', farmId: 'farm-1', sourceId: 'src-1', consumptionLiters: 150, consumptionDate: '2026-09-01T08:00:00Z' },
]

beforeEach(() => {
  vi.clearAllMocks()
  waterConsumptionApi.getConsumptions.mockResolvedValue([...CONSUMPTIONS])
  waterSourceApi.getSources.mockResolvedValue([{ id: 'src-1', farmId: 'farm-1', name: 'Borehole A' }])
  waterQuotaApi.getUsage.mockResolvedValue([])
})

const props = { initials: 'YN' }

describe('ConsumptionView (read-only - IoT sensors)', () => {
  it('displays the consumptions and the chart', async () => {
    render(<ConsumptionView {...props} />)

    await waitFor(() => expect(screen.getByTestId('area-chart')).toBeTruthy())
    expect(screen.getAllByText('150 L').length).toBeGreaterThan(0)
    expect(screen.getByText('Peak')).toBeTruthy()
  })

  it('displays an empty state with an empty database', async () => {
    waterConsumptionApi.getConsumptions.mockResolvedValue([])

    render(<ConsumptionView {...props} />)

    await waitFor(() => expect(screen.getByText('No consumption')).toBeTruthy())
    expect(screen.getAllByText(/IoT sensor/).length).toBeGreaterThan(0)
  })

  it('exposes no consumption input (IoT sensor data)', async () => {
    render(<ConsumptionView {...props} />)
    await screen.findByTestId('area-chart')

    // The only form of the view concerns the monthly quotas, not consumption input.
    const forms = document.querySelectorAll('form')
    expect(forms.length).toBe(1)
    expect(forms[0].textContent).toMatch(/Set quota/)
    expect(screen.queryByText('Edit')).toBeNull()
    expect(screen.queryByTitle('Delete')).toBeNull()
  })

  it('displays the monthly quota tracking with its status', async () => {
    waterQuotaApi.getUsage.mockResolvedValue([
      {
        quota_id: 'q1',
        target_type: 'farm',
        target_id: 'farm-1',
        target_name: 'North Farm',
        quota_month: '2026-09-01',
        quota_liters: 1000,
        used_liters: 850,
        remaining_liters: 150,
        usage_percentage: 85,
        status: 'warning',
      },
    ])

    render(<ConsumptionView {...props} />)

    // « North Farm » also appears in the farm filter: we target the quota panel.
    await waitFor(() => expect(screen.getAllByText('North Farm').length).toBeGreaterThan(1))
    expect(screen.getByText('80% of quota reached')).toBeTruthy()
    expect(screen.getByText(/85% of quota used/)).toBeTruthy()
    expect(screen.getByTestId('bullet-chart')).toBeTruthy()
  })

  it('reports an exceeded quota', async () => {
    waterQuotaApi.getUsage.mockResolvedValue([
      {
        quota_id: 'q2',
        target_type: 'zone',
        target_id: 'zone-1',
        target_name: 'South Zone',
        quota_month: '2026-09-01',
        quota_liters: 500,
        used_liters: 640,
        remaining_liters: -140,
        usage_percentage: 128,
        status: 'exceeded',
      },
    ])

    render(<ConsumptionView {...props} />)

    await waitFor(() => expect(screen.getByText('Quota exceeded')).toBeTruthy())
    expect(screen.getByText(/over by 140 L/)).toBeTruthy()
  })

  it('filters by farm', async () => {
    waterConsumptionApi.getConsumptions.mockResolvedValue([
      ...CONSUMPTIONS,
      { id: 'c2', farmId: 'farm-2', sourceId: 'src-1', consumptionLiters: 90, consumptionDate: '2026-09-02T08:00:00Z' },
    ])

    render(<ConsumptionView {...props} />)
    await screen.findByTestId('area-chart')

    const filterSelects = document.querySelectorAll('.ws-filters select')
    fireEvent.change(filterSelects[0], { target: { value: 'farm-1' } })

    await waitFor(() => expect(screen.queryByText('90 L')).toBeNull())
    expect(screen.getAllByText('150 L').length).toBeGreaterThan(0)
  })
})