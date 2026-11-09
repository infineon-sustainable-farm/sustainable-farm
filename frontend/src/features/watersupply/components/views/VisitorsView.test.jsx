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
    liveConsumption: { liters: 12.5, windowMinutes: 15 },
    activeUnavailability: [],
  })
  iotCommandApi.getCommands.mockResolvedValue([])
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
    expect(screen.getByText('12.5 L')).toBeTruthy()

    fireEvent.change(screen.getByLabelText('Zone shown'), { target: { value: 'zone-1' } })
    await waitFor(() => expect(visitorApi.getSummary).toHaveBeenCalledWith({ sourceId: undefined, zoneId: 'zone-1' }))
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
