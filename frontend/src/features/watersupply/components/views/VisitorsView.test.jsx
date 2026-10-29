// @vitest-environment jsdom
// DOM environment and between-test cleanup declared in the file: the module requires
// no shared Vitest configuration (see SourcesView.test.jsx).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { controlByLabel, renderView as render } from './testRender'
import { VisitorsView } from './VisitorsView'
import { iotCommandApi, visitorApi } from '../../api/watersupplyApi'

afterEach(cleanup)

vi.mock('../../api/watersupplyApi', () => ({
  visitorApi: {
    getSummary: vi.fn(),
    getStops: vi.fn(),
    createStop: vi.fn(),
    deleteStop: vi.fn(),
    getSafetyBriefing: vi.fn(),
    publishSafetyBriefing: vi.fn(),
  },
  iotCommandApi: {
    sendCommand: vi.fn(),
    getCommands: vi.fn(),
  },
}))

vi.mock('../../hooks/useFarms', () => ({
  useZones: () => ({ zones: [{ id: 'zone-1', name: 'Demo zone' }] }),
}))

vi.mock('../../hooks/useWaterData', () => ({
  useWaterSources: () => ({ sources: [{ id: 'src-1', name: 'Rain tank' }] }),
}))

const DEFAULT_BRIEFING = {
  version: 'default', validated: false, text: 'Default text', allowedZones: ['reservoir surroundings'],
  forbiddenZones: ['electric pump room'], risks: ['slippery ground near the basin'], maxCapacity: 10, guideRequired: true,
}

beforeEach(() => {
  vi.clearAllMocks()
  visitorApi.getSafetyBriefing.mockResolvedValue(DEFAULT_BRIEFING)
  visitorApi.getSummary.mockResolvedValue({
    sourceLevel: null,
    waterSavingsPercentage: 18,
    irrigationStatus: null,
    liveConsumption: { liters: 30, flowRateLitersPerMinute: 2, windowMinutes: 15 },
    activeUnavailability: [],
  })
  iotCommandApi.getCommands.mockResolvedValue([])
  visitorApi.getStops.mockResolvedValue([
    { id: 'stop-1', stopName: 'Smart drip irrigation', zoneId: 'zone-1', sourceId: null },
  ])
})

const props = { notify: vi.fn() }

describe('VisitorsView', () => {
  it('flags the default briefing as not validated', async () => {
    render(<VisitorsView {...props} />)

    await waitFor(() => expect(screen.getByText('Default text, not validated')).toBeTruthy())
    expect(screen.getByText('electric pump room')).toBeTruthy()
  })

  it('publishes a new briefing version from one entry per line', async () => {
    visitorApi.publishSafetyBriefing.mockResolvedValue({})
    render(<VisitorsView {...props} />)
    fireEvent.click(await screen.findByText('Publish a new version'))
    const form = screen.getByRole('button', { name: 'Publish' }).closest('form')

    fireEvent.change(controlByLabel(form, 'Forbidden areas'), { target: { value: 'pump room\n\n chemical storage ' } })
    fireEvent.change(controlByLabel(form, 'Maximum group size'), { target: { value: '8' } })
    fireEvent.submit(form)

    await waitFor(() => expect(visitorApi.publishSafetyBriefing).toHaveBeenCalledWith({
      version: null,
      text: 'Default text',
      allowedZones: ['reservoir surroundings'],
      forbiddenZones: ['pump room', 'chemical storage'],
      risks: ['slippery ground near the basin'],
      maxCapacity: 8,
      guideRequired: true,
    }))
    await waitFor(() => expect(visitorApi.getSafetyBriefing).toHaveBeenCalledTimes(2))
    expect(props.notify).toHaveBeenCalledWith('Safety briefing published')
  })

  it('previews what a visitor screen receives', async () => {
    render(<VisitorsView {...props} />)

    await waitFor(() => expect(screen.getByText('18%')).toBeTruthy())
    // The short window only gives the instantaneous flow; the visit total uses the time slot.
    expect(screen.getByText('2 L/min')).toBeTruthy()

    fireEvent.change(screen.getByLabelText('Zone shown'), { target: { value: 'zone-1' } })
    await waitFor(() => expect(visitorApi.getSummary).toHaveBeenCalledWith({ sourceId: undefined, zoneId: 'zone-1' }))

    // Visitor Management speaks in stop names: the preview can use them too.
    fireEvent.change(await screen.findByLabelText('Visit stop'), { target: { value: 'Smart drip irrigation' } })
    await waitFor(() => expect(visitorApi.getSummary).toHaveBeenCalledWith({ stop: 'Smart drip irrigation' }))
  })

  it('maps a Visitor Management stop to the zone it shows', async () => {
    visitorApi.createStop.mockResolvedValue({ id: 'stop-2' })
    render(<VisitorsView {...props} />)
    await screen.findByText('1 water stop(s)')
    const form = screen.getByRole('button', { name: 'Map the stop' }).closest('form')

    fireEvent.submit(form)
    await waitFor(() => expect(screen.getByText(/exactly as Visitor Management shows it/)).toBeTruthy())

    fireEvent.change(controlByLabel(form, 'Stop name'), { target: { value: ' Rain tank tour ' } })
    fireEvent.submit(form)
    await waitFor(() => expect(screen.getByText(/the zone, the tank, or both/)).toBeTruthy())

    fireEvent.change(controlByLabel(form, 'Zone shown'), { target: { value: 'zone-1' } })
    fireEvent.submit(form)
    await waitFor(() => expect(visitorApi.createStop)
      .toHaveBeenCalledWith({ stopName: 'Rain tank tour', zoneId: 'zone-1', sourceId: null }))
    await waitFor(() => expect(visitorApi.getStops).toHaveBeenCalledTimes(2))
  })

  it('says plainly that the demo valve did not move while no gateway exists', async () => {
    iotCommandApi.sendCommand.mockRejectedValue(Object.assign(new Error('not configured'), { status: 503 }))
    iotCommandApi.getCommands.mockResolvedValue([
      { commandId: 'cmd-1', command: 'OPEN_VALVE', status: 'rejected', requestedAt: '2026-10-22T09:00:00Z' },
    ])
    render(<VisitorsView {...props} />)

    fireEvent.change(screen.getByLabelText('Valve device id'), { target: { value: 'valve-demo-01' } })
    fireEvent.click(screen.getByRole('button', { name: /Open 10 s/ }))

    await waitFor(() => expect(iotCommandApi.sendCommand).toHaveBeenCalledWith('valve-demo-01',
      { command: 'OPEN_VALVE', durationSeconds: 10, reason: 'visitor_demo' }))
    await waitFor(() => expect(props.notify).toHaveBeenCalledWith('No device gateway yet: the command was recorded but not sent.'))
    await waitFor(() => expect(screen.getByText('Not sent')).toBeTruthy())
  })
})
