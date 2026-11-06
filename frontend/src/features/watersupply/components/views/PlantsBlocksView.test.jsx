// @vitest-environment jsdom
// DOM environment and between-test cleanup declared in the file: the module requires
// no shared Vitest configuration (see SourcesView.test.jsx).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { controlByLabel, renderView as render } from './testRender'
import { PlantsBlocksView } from './PlantsBlocksView'
import { plantsIntegrationApi, zoneBlockMappingApi } from '../../api/watersupplyApi'

afterEach(cleanup)

vi.mock('../../api/watersupplyApi', () => ({
  zoneBlockMappingApi: {
    getMappings: vi.fn(),
    createMapping: vi.fn(),
    deleteMapping: vi.fn(),
  },
  plantsIntegrationApi: {
    getRainfall: vi.fn(),
    getIrrigationNeed: vi.fn(),
  },
}))

vi.mock('../../hooks/useFarms', () => ({
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'North Farm' }] }),
  useFields: () => ({ fields: [{ id: 'field-1', farmId: 'farm-1' }] }),
  useZones: () => ({ zones: [
    { id: 'zone-1', name: 'Drip Zone A', fieldId: 'field-1' },
    { id: 'zone-2', name: 'Drip Zone B', fieldId: 'field-1' },
    { id: 'zone-orphan', name: 'Orphan zone', fieldId: 'field-unknown' },
  ] }),
}))

beforeEach(() => {
  vi.clearAllMocks()
  zoneBlockMappingApi.getMappings.mockResolvedValue([
    { id: 'map-1', zoneId: 'zone-1', blockCode: 'A', farmId: 'farm-1' },
    { id: 'map-2', zoneId: 'zone-2', blockCode: 'B', farmId: 'farm-1' },
    { id: 'map-3', zoneId: 'zone-2', blockCode: 'C', farmId: 'farm-1' },
  ])
  plantsIntegrationApi.getRainfall.mockResolvedValue({ blockCode: 'A', rainfallMm: 15, gaugeCount: 2 })
  plantsIntegrationApi.getIrrigationNeed.mockResolvedValue({
    blockCode: 'A', needLiters: 93750, baseNeedLiters: 93750, et0Mm: 5, growthPhase: 'full production',
    growthPhaseSource: 'plants_calendar', phaseFactor: 1, warnings: ['phase_factors_not_validated'],
  })
})

const props = { notify: vi.fn() }

describe('PlantsBlocksView', () => {
  it('lists the links and flags a zone shared by several blocks', async () => {
    render(<PlantsBlocksView {...props} />)

    // The zone names are also options of the form: wait for the links themselves.
    await screen.findByText('3 block(s), 3 link(s)')
    expect(screen.getAllByText('Drip Zone A').length).toBeGreaterThan(1)
    // Zone B waters blocks B and C: Plants gets multipleBlockCodes=true for its volumes.
    expect(screen.getAllByText('several blocks')).toHaveLength(2)
  })

  it('shows what Plants reads for the selected block', async () => {
    render(<PlantsBlocksView {...props} />)

    await waitFor(() => expect(screen.getByText('15 mm')).toBeTruthy())
    expect(screen.getByText('93750 L')).toBeTruthy()
    expect(screen.getByText(/full production × 1/)).toBeTruthy()
    expect(screen.getByText(/Phase factors are still 1.0/)).toBeTruthy()
    expect(plantsIntegrationApi.getRainfall).toHaveBeenCalledWith('A', expect.stringMatching(/-01$/), expect.any(String))
    expect(plantsIntegrationApi.getIrrigationNeed).toHaveBeenCalledWith(expect.objectContaining({ blockCode: 'A', growthPhase: undefined }))

    fireEvent.change(controlByLabel(document.body, 'Growth phase used'), { target: { value: 'establishment' } })
    await waitFor(() => expect(plantsIntegrationApi.getIrrigationNeed)
      .toHaveBeenCalledWith(expect.objectContaining({ blockCode: 'A', growthPhase: 'establishment' })))
  })

  it('links a zone with the farm of its field', async () => {
    zoneBlockMappingApi.createMapping.mockResolvedValue({ id: 'map-4', blockCode: 'D1' })
    render(<PlantsBlocksView {...props} />)
    await screen.findByText('3 block(s), 3 link(s)')
    const form = document.querySelector('form')

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-1' } })
    fireEvent.change(controlByLabel(form, 'Plants block code'), { target: { value: ' d1 ' } })
    fireEvent.submit(form)

    await waitFor(() => expect(zoneBlockMappingApi.createMapping)
      .toHaveBeenCalledWith({ zoneId: 'zone-1', blockCode: 'd1', farmId: 'farm-1' }))
    await waitFor(() => expect(props.notify).toHaveBeenCalledWith('Zone linked to block D1'))
  })

  it('refuses a block code that Plants would not accept', async () => {
    render(<PlantsBlocksView {...props} />)
    await screen.findByText('3 block(s), 3 link(s)')
    const form = document.querySelector('form')

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-1' } })
    fireEvent.change(controlByLabel(form, 'Plants block code'), { target: { value: 'Block A' } })
    fireEvent.submit(form)

    await waitFor(() => expect(screen.getByText(/short code such as A or B2/)).toBeTruthy())
    expect(zoneBlockMappingApi.createMapping).not.toHaveBeenCalled()
  })

  it('explains a zone whose farm cannot be resolved and a duplicate link', async () => {
    zoneBlockMappingApi.createMapping.mockRejectedValue(Object.assign(new Error('conflict'), { status: 409 }))
    render(<PlantsBlocksView {...props} />)
    await screen.findByText('3 block(s), 3 link(s)')
    const form = document.querySelector('form')

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-orphan' } })
    fireEvent.change(controlByLabel(form, 'Plants block code'), { target: { value: 'A' } })
    fireEvent.submit(form)
    await waitFor(() => expect(screen.getByText(/farm of this zone is unknown/)).toBeTruthy())

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-1' } })
    fireEvent.submit(form)
    await waitFor(() => expect(screen.getByText('This zone is already linked to this block.')).toBeTruthy())
  })
})
