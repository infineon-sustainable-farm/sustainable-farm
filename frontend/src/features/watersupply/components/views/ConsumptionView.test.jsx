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
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'Ferme Nord' }] }),
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'Zone Sud' }] }),
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
  waterSourceApi.getSources.mockResolvedValue([{ id: 'src-1', farmId: 'farm-1', name: 'Forage A' }])
  waterQuotaApi.getUsage.mockResolvedValue([])
})

const props = { initials: 'YN' }

describe('ConsumptionView (lecture seule - capteurs IoT)', () => {
  it('affiche les consommations et le graphique', async () => {
    render(<ConsumptionView {...props} />)

    await waitFor(() => expect(screen.getByTestId('area-chart')).toBeTruthy())
    expect(screen.getAllByText('150 L').length).toBeGreaterThan(0)
    expect(screen.getByText('Pic')).toBeTruthy()
  })

  it('affiche un état vide avec une base vide', async () => {
    waterConsumptionApi.getConsumptions.mockResolvedValue([])

    render(<ConsumptionView {...props} />)

    await waitFor(() => expect(screen.getByText('Aucune consommation')).toBeTruthy())
    expect(screen.getAllByText(/capteurs IoT/).length).toBeGreaterThan(0)
  })

  it('n expose aucune saisie de consommation (données capteurs)', async () => {
    render(<ConsumptionView {...props} />)
    await screen.findByTestId('area-chart')

    // Le seul formulaire de la vue concerne les quotas mensuels, pas la saisie de consommation.
    const forms = document.querySelectorAll('form')
    expect(forms.length).toBe(1)
    expect(forms[0].textContent).toMatch(/Définir le quota/)
    expect(screen.queryByText('Modifier')).toBeNull()
    expect(screen.queryByTitle('Supprimer')).toBeNull()
  })

  it('affiche le suivi des quotas mensuels avec leur statut', async () => {
    waterQuotaApi.getUsage.mockResolvedValue([
      {
        quota_id: 'q1',
        target_type: 'farm',
        target_id: 'farm-1',
        target_name: 'Ferme Nord',
        quota_month: '2026-09-01',
        quota_liters: 1000,
        used_liters: 850,
        remaining_liters: 150,
        usage_percentage: 85,
        status: 'warning',
      },
    ])

    render(<ConsumptionView {...props} />)

    // « Ferme Nord » apparait aussi dans le filtre des fermes : on cible le panneau quotas.
    await waitFor(() => expect(screen.getAllByText('Ferme Nord').length).toBeGreaterThan(1))
    expect(screen.getByText('80 % du quota atteint')).toBeTruthy()
    expect(screen.getByText(/85 % du quota utilisé/)).toBeTruthy()
    expect(screen.getByTestId('bullet-chart')).toBeTruthy()
  })

  it('signale un quota dépassé', async () => {
    waterQuotaApi.getUsage.mockResolvedValue([
      {
        quota_id: 'q2',
        target_type: 'zone',
        target_id: 'zone-1',
        target_name: 'Zone Sud',
        quota_month: '2026-09-01',
        quota_liters: 500,
        used_liters: 640,
        remaining_liters: -140,
        usage_percentage: 128,
        status: 'exceeded',
      },
    ])

    render(<ConsumptionView {...props} />)

    await waitFor(() => expect(screen.getByText('Quota dépassé')).toBeTruthy())
    expect(screen.getByText(/dépassement de 140 L/)).toBeTruthy()
  })

  it('filtre par ferme', async () => {
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