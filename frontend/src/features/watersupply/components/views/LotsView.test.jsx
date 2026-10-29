// @vitest-environment jsdom
// DOM environment and between-test cleanup declared in the file: the module requires
// no shared Vitest configuration (see SourcesView.test.jsx).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { controlByLabel, renderView as render } from './testRender'
import { LotsView } from './LotsView'
import { productTransformationApi } from '../../api/watersupplyApi'

afterEach(cleanup)

vi.mock('../../api/watersupplyApi', () => ({
  productTransformationApi: {
    getConsumptionByLot: vi.fn(),
    getQualityCompliance: vi.fn(),
    attachLotUsage: vi.fn(),
    forecastAvailability: vi.fn(),
  },
}))

vi.mock('../../hooks/useWaterData', () => ({
  useWaterSources: () => ({ sources: [{ id: 'src-1', name: 'Washing tank' }] }),
}))

const LOT = {
  lotId: 'LOT-1',
  totalLiters: 150,
  byActivity: { WASHING: 150 },
  items: [{ consumptionId: 'c1', activity: 'WASHING', sourceId: 'src-1', liters: 150, consumptionDate: '2026-10-13T08:10:00Z' }],
}

const COMPLIANCE = {
  lotId: 'LOT-1', from: '2026-10-13T08:00:00Z', to: '2026-10-13T10:00:00Z', sourceIds: ['src-1'],
  status: 'compliant', compliant: true, rule: { phMin: 6, phMax: 7.5, turbidityMaxNtu: 5 }, tests: [{}],
  validityDays: 7, sources: [{ sourceId: 'src-1', status: 'compliant', testCount: 1 }],
}

beforeEach(() => {
  vi.clearAllMocks()
  productTransformationApi.getConsumptionByLot.mockResolvedValue(LOT)
  productTransformationApi.getQualityCompliance.mockResolvedValue(COMPLIANCE)
})

const props = { notify: vi.fn() }

function lookUp(lotId) {
  fireEvent.change(screen.getByLabelText('Lot identifier'), { target: { value: lotId } })
  fireEvent.click(screen.getByRole('button', { name: /Look up/ }))
}

describe('LotsView', () => {
  it('shows the water used by a lot and the quality verdict', async () => {
    render(<LotsView {...props} />)
    expect(screen.getByText('No lot selected')).toBeTruthy()

    lookUp(' LOT-1 ')

    await waitFor(() => expect(screen.getByText('Water used by LOT-1')).toBeTruthy())
    expect(productTransformationApi.getConsumptionByLot).toHaveBeenCalledWith('LOT-1')
    expect(screen.getByText('Washing: 150 L')).toBeTruthy()
    await waitFor(() => expect(screen.getByText('Compliant')).toBeTruthy())
    expect(screen.getAllByText('Washing tank').length).toBeGreaterThan(0)
    expect(screen.getByText('Washing tank: Compliant')).toBeTruthy()
    expect(screen.getByText(/the 7 days before the lot/)).toBeTruthy()
  })

  it('never presents a lot without quality test as compliant', async () => {
    productTransformationApi.getQualityCompliance.mockResolvedValue({
      ...COMPLIANCE, status: 'no_test', compliant: false, tests: [{}],
      sources: [{ sourceId: 'src-1', status: 'compliant' }, { sourceId: 'src-2', status: 'no_test' }],
    })
    render(<LotsView {...props} />)

    lookUp('LOT-1')

    // One source of the lot was never tested: the lot cannot be compliant.
    await waitFor(() => expect(screen.getAllByText('No test in the window').length).toBeGreaterThan(0))
    expect(screen.getByText(/Source src-2: No test in the window/)).toBeTruthy()
    expect(screen.getByText(/cannot be declared compliant/)).toBeTruthy()
  })

  it('requires the source before attributing a window', async () => {
    render(<LotsView {...props} />)
    const form = screen.getByRole('button', { name: 'Attribute' }).closest('form')

    fireEvent.change(controlByLabel(form, 'Lot'), { target: { value: 'LOT-2' } })
    fireEvent.submit(form)

    await waitFor(() => expect(screen.getByText('Select the source used by the lot.')).toBeTruthy())
    expect(productTransformationApi.attachLotUsage).not.toHaveBeenCalled()
  })

  it('attributes a window to a lot, reports the warnings and shows the lot', async () => {
    productTransformationApi.attachLotUsage.mockResolvedValue({
      lotId: 'LOT-1', attachedCount: 2, attachedLiters: 150, skippedOtherLotCount: 1, warnings: ['window_overlaps_other_lot'],
    })
    render(<LotsView {...props} />)
    const form = screen.getByRole('button', { name: 'Attribute' }).closest('form')

    fireEvent.change(controlByLabel(form, 'Lot'), { target: { value: 'LOT-1' } })
    fireEvent.change(controlByLabel(form, 'Activity'), { target: { value: 'CUTTING' } })
    fireEvent.change(controlByLabel(form, 'Source'), { target: { value: 'src-1' } })
    fireEvent.change(controlByLabel(form, 'From'), { target: { value: '2026-10-13T08:00' } })
    fireEvent.change(controlByLabel(form, 'To'), { target: { value: '2026-10-13T10:00' } })
    fireEvent.submit(form)

    await waitFor(() => expect(productTransformationApi.attachLotUsage).toHaveBeenCalledWith({
      lotId: 'LOT-1',
      activity: 'CUTTING',
      sourceId: 'src-1',
      startAt: new Date('2026-10-13T08:00').toISOString(),
      endAt: new Date('2026-10-13T10:00').toISOString(),
    }))
    await waitFor(() => expect(screen.getByText(/already belong to another lot/)).toBeTruthy())
    expect(props.notify).toHaveBeenCalledWith('2 consumption(s) attributed to LOT-1')
    await waitFor(() => expect(productTransformationApi.getConsumptionByLot).toHaveBeenCalledWith('LOT-1'))
  })

  it('forecasts the availability of a source for planned batches', async () => {
    productTransformationApi.forecastAvailability.mockResolvedValue({
      sourceId: 'src-1', horizonDays: 7, currentLevelLiters: 2500, batchDemandLiters: 2200, irrigationDemandLiters: 0,
      forecastDemandLiters: 2200, availableAfterForecastLiters: 300, bufferMinLiters: 500, bufferStatus: 'tight',
      batches: [], warnings: [], parametersStatus: 'unvalidated_defaults',
    })
    render(<LotsView {...props} />)
    const form = screen.getByRole('button', { name: 'Forecast' }).closest('form')

    fireEvent.change(controlByLabel(form, 'Source'), { target: { value: 'src-1' } })
    fireEvent.change(screen.getByLabelText('Batch 1 lot'), { target: { value: 'LOT-9' } })
    fireEvent.change(screen.getByLabelText('Batch 1 size'), { target: { value: '1100' } })
    fireEvent.click(screen.getByLabelText(/Include the farm's scheduled irrigations/))
    fireEvent.submit(form)

    await waitFor(() => expect(productTransformationApi.forecastAvailability).toHaveBeenCalledWith({
      sourceId: 'src-1',
      horizonDays: 7,
      includeIrrigation: false,
      batches: [{ lotId: 'LOT-9', activity: 'WASHING', sizeKg: 1100, plannedAt: null }],
    }))
    await waitFor(() => expect(screen.getByText('Below the minimum buffer')).toBeTruthy())
    expect(screen.getByText('300 L')).toBeTruthy()
    expect(screen.getByText(/working defaults, to be confirmed/)).toBeTruthy()
  })
})
