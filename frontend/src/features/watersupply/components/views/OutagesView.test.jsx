// @vitest-environment jsdom
// DOM environment and between-test cleanup declared in the file: the module requires
// no shared Vitest configuration (see SourcesView.test.jsx).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { controlByLabel, renderView as render } from './testRender'
import { OutagesView } from './OutagesView'
import { outageApi } from '../../api/watersupplyApi'

afterEach(cleanup)

vi.mock('../../api/watersupplyApi', () => ({
  outageApi: {
    getOutages: vi.fn(),
    createOutage: vi.fn(),
    updateOutage: vi.fn(),
    deleteOutage: vi.fn(),
  },
}))

vi.mock('../../hooks/useFarms', () => ({
  useFarms: () => ({ farms: [{ id: 'farm-1', name: 'North Farm' }] }),
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'Drip Zone A' }] }),
}))

vi.mock('../../hooks/useWaterData', () => ({
  useWaterSources: () => ({ sources: [{ id: 'src-1', name: 'Main borehole' }] }),
}))

const HOUR = 3600 * 1000
const iso = (offsetMs) => new Date(Date.now() + offsetMs).toISOString()

const OUTAGES = [
  { id: 'out-1', scope: 'zone', scopeId: 'zone-1', startAt: iso(-HOUR), endAt: iso(HOUR), reason: 'Filter cleaning', status: 'planned' },
  { id: 'out-2', scope: 'farm', scopeId: 'farm-1', startAt: iso(48 * HOUR), endAt: iso(50 * HOUR), reason: 'Pump repair', status: 'planned' },
  { id: 'out-3', scope: 'source', scopeId: 'src-1', startAt: iso(24 * HOUR), endAt: iso(25 * HOUR), reason: 'Old plan', status: 'cancelled' },
]

beforeEach(() => {
  vi.clearAllMocks()
  outageApi.getOutages.mockResolvedValue([...OUTAGES])
})

const props = { notify: vi.fn() }

describe('OutagesView', () => {
  it('shows current and upcoming periods with their target, hiding cancelled ones by default', async () => {
    render(<OutagesView {...props} />)

    await screen.findByText('Filter cleaning')
    expect(screen.getByText('In force')).toBeTruthy()
    expect(screen.getByText('Upcoming')).toBeTruthy()
    expect(screen.getAllByText('North Farm').length).toBeGreaterThan(0)
    expect(screen.queryByText('Old plan')).toBeNull()

    fireEvent.click(screen.getByText('Cancelled', { selector: 'button' }))
    await waitFor(() => expect(screen.getByText('Old plan')).toBeTruthy())
  })

  it('plans a zone outage that postpones its irrigations', async () => {
    outageApi.createOutage.mockResolvedValue({ id: 'out-4' })
    render(<OutagesView {...props} />)
    await screen.findByText('Filter cleaning')
    const form = document.querySelector('form')

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-1' } })
    fireEvent.change(controlByLabel(form, 'Start'), { target: { value: '2026-11-02T08:00' } })
    fireEvent.change(controlByLabel(form, 'End'), { target: { value: '2026-11-02T12:00' } })
    fireEvent.change(controlByLabel(form, 'Reason'), { target: { value: ' Valve swap ' } })
    fireEvent.submit(form)

    await waitFor(() => expect(outageApi.createOutage).toHaveBeenCalledWith({
      scope: 'zone',
      scopeId: 'zone-1',
      startAt: new Date('2026-11-02T08:00').toISOString(),
      endAt: new Date('2026-11-02T12:00').toISOString(),
      reason: 'Valve swap',
      status: 'planned',
      postponeIrrigations: true,
    }))
    await waitFor(() => expect(outageApi.getOutages).toHaveBeenCalledTimes(2))
    expect(props.notify).toHaveBeenCalledWith('Unavailability planned')
  })

  it('refuses a period ending before it starts', async () => {
    render(<OutagesView {...props} />)
    await screen.findByText('Filter cleaning')
    const form = document.querySelector('form')

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-1' } })
    fireEvent.change(controlByLabel(form, 'Start'), { target: { value: '2026-11-02T12:00' } })
    fireEvent.change(controlByLabel(form, 'End'), { target: { value: '2026-11-02T08:00' } })
    fireEvent.submit(form)

    await waitFor(() => expect(screen.getByText('The end must be after the start.')).toBeTruthy())
    expect(outageApi.createOutage).not.toHaveBeenCalled()
  })

  it('shows the overlap refused by the backend', async () => {
    outageApi.createOutage.mockRejectedValue(new Error('Water unavailability overlaps an existing period on the same scope'))
    render(<OutagesView {...props} />)
    await screen.findByText('Filter cleaning')
    const form = document.querySelector('form')

    fireEvent.change(controlByLabel(form, 'Zone'), { target: { value: 'zone-1' } })
    fireEvent.change(controlByLabel(form, 'Start'), { target: { value: '2026-11-02T08:00' } })
    fireEvent.change(controlByLabel(form, 'End'), { target: { value: '2026-11-02T12:00' } })
    fireEvent.submit(form)

    await waitFor(() => expect(screen.getByText(/overlaps an existing period/)).toBeTruthy())
  })

  it('never offers to postpone irrigations for a source, which has no zone', async () => {
    render(<OutagesView {...props} />)
    await screen.findByText('Filter cleaning')

    fireEvent.change(controlByLabel(document.querySelector('form'), 'Unavailable'), { target: { value: 'source' } })

    expect(screen.queryByText(/Postpone the irrigations/)).toBeNull()
    expect(screen.getByText(/cannot be postponed automatically/)).toBeTruthy()
  })

  it('cancels a period instead of deleting it', async () => {
    outageApi.updateOutage.mockResolvedValue({})
    render(<OutagesView {...props} />)
    await screen.findByText('Filter cleaning')

    fireEvent.click(screen.getAllByText('Cancel', { selector: 'td button' })[0])

    await waitFor(() => expect(outageApi.updateOutage).toHaveBeenCalledWith('out-1', expect.objectContaining({
      scope: 'zone', scopeId: 'zone-1', status: 'cancelled', postponeIrrigations: false,
    })))
  })
})
